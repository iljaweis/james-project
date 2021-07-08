/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageModseqTable.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageModseqTable.NEXT_MODSEQ;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageModseqTable.TABLE_NAME;
import static org.apache.james.util.ReactorUtils.publishIfPresent;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.init.configuration.CassandraConsistenciesConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.mailbox.ModSeq;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.mail.ModSeqProvider;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * Copy of {@link org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider} without LWT
 *
 * Warning ModSeq generated that way are not to be trusted and duplicates upon concurrent calls might exist.
 *
 * Changes to the original class are documented as JavaDoc
 */
public class NoLWTModSeqProvider implements ModSeqProvider {
    private static <T> T unbox(Supplier<T> supplier) throws MailboxException {
        try {
            return supplier.get();
        } catch (CompletionException e) {
            if (e.getCause() instanceof CassandraModSeqProvider.ExceptionRelay) {
                throw ((CassandraModSeqProvider.ExceptionRelay) e.getCause()).getUnderlying();
            }
            throw e;
        }
    }

    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final long maxModSeqRetries;
    private final PreparedStatement select;
    private final PreparedStatement update;
    private final PreparedStatement insert;
    private final ConsistencyLevel consistencyLevel;

    /**
     * Uses regular consistency level
     */
    @Inject
    public NoLWTModSeqProvider(Session session, CassandraConfiguration cassandraConfiguration,
                                   CassandraConsistenciesConfiguration consistenciesConfiguration) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.consistencyLevel = consistenciesConfiguration.getRegular();
        this.maxModSeqRetries = cassandraConfiguration.getModSeqMaxRetry();
        this.insert = prepareInsert(session);
        this.update = prepareUpdate(session);
        this.select = prepareSelect(session);
    }

    /**
     * Remove `IF NOT EXIST`
     */
    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(NEXT_MODSEQ, bindMarker(NEXT_MODSEQ))
            .value(MAILBOX_ID, bindMarker(MAILBOX_ID)));
    }

    /**
     * Remove `IF ...`
     */
    private PreparedStatement prepareUpdate(Session session) {
        return session.prepare(update(TABLE_NAME)
            .with(set(NEXT_MODSEQ, bindMarker(NEXT_MODSEQ)))
            .where(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select(NEXT_MODSEQ)
            .from(TABLE_NAME)
            .where(eq(MAILBOX_ID, bindMarker(MAILBOX_ID))));
    }

    @Override
    public ModSeq nextModSeq(Mailbox mailbox) throws MailboxException {
        return nextModSeqReactive(mailbox.getMailboxId())
            .blockOptional()
            .orElseThrow(() -> new MailboxException("Can not retrieve modseq for " + mailbox.getMailboxId()));
    }

    @Override
    public ModSeq nextModSeq(MailboxId mailboxId) throws MailboxException {
        return nextModSeqReactive(mailboxId)
            .blockOptional()
            .orElseThrow(() -> new MailboxException("Can not retrieve modseq for " + mailboxId));
    }

    @Override
    public ModSeq highestModSeq(Mailbox mailbox) throws MailboxException {
        return highestModSeq(mailbox.getMailboxId());
    }

    @Override
    public ModSeq highestModSeq(MailboxId mailboxId) throws MailboxException {
        return unbox(() -> findHighestModSeq((CassandraId) mailboxId).block().orElse(ModSeq.first()));
    }

    private Mono<Optional<ModSeq>> findHighestModSeq(CassandraId mailboxId) {
        return cassandraAsyncExecutor.executeSingleRowOptional(
            select.bind()
                .setUUID(MAILBOX_ID, mailboxId.asUuid())
                .setConsistencyLevel(consistencyLevel))
            .map(maybeRow -> maybeRow.map(row -> ModSeq.of(row.getLong(NEXT_MODSEQ))));
    }

    private Mono<ModSeq> tryInsertModSeq(CassandraId mailboxId, ModSeq modSeq) {
        ModSeq nextModSeq = modSeq.next();
        return cassandraAsyncExecutor.executeReturnApplied(
            insert.bind()
                .setUUID(MAILBOX_ID, mailboxId.asUuid())
                .setLong(NEXT_MODSEQ, nextModSeq.asLong()))
            .map(success -> successToModSeq(nextModSeq, success))
            .handle(publishIfPresent());
    }

    /**
     * Remove condition binding
     */
    private Mono<ModSeq> tryUpdateModSeq(CassandraId mailboxId, ModSeq modSeq) {
        ModSeq nextModSeq = modSeq.next();
        return cassandraAsyncExecutor.executeReturnApplied(
            update.bind()
                .setUUID(MAILBOX_ID, mailboxId.asUuid())
                .setLong(NEXT_MODSEQ, nextModSeq.asLong()))
            .map(success -> successToModSeq(nextModSeq, success))
            .handle(publishIfPresent());
    }

    private Optional<ModSeq> successToModSeq(ModSeq modSeq, Boolean success) {
        if (success) {
            return Optional.of(modSeq);
        }
        return Optional.empty();
    }

    @Override
    public Mono<ModSeq> nextModSeqReactive(MailboxId mailboxId) {
        CassandraId cassandraId = (CassandraId) mailboxId;
        Duration firstBackoff = Duration.ofMillis(10);

        return findHighestModSeq(cassandraId)
            .flatMap(maybeHighestModSeq -> maybeHighestModSeq
                .map(highestModSeq -> tryUpdateModSeq(cassandraId, highestModSeq))
                .orElseGet(() -> tryInsertModSeq(cassandraId, ModSeq.first())))
            .single()
            .retryWhen(Retry.backoff(maxModSeqRetries, firstBackoff).scheduler(Schedulers.elastic()));
    }
}
