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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.backends.cassandra.StatementRecorder;
import org.apache.james.backends.cassandra.TestingSession;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.SessionWithInitializedTablesFactory;
import org.apache.james.blob.cassandra.cache.CassandraBlobCacheModule;
import org.apache.james.core.Username;
import org.apache.james.jmap.JMAPTestingConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.GuiceProbe;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.SpoolerProbe;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.datastax.driver.core.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

class WithCacheMutableTest implements MailsShouldBeWellReceived {
    public static class TestingSessionProbe implements GuiceProbe {
        private final TestingSession testingSession;

        @Inject
        private TestingSessionProbe(TestingSession testingSession) {
            this.testingSession = testingSession;
        }

        public TestingSession getTestingSession() {
            return testingSession;
        }
    }

    public static class TestingSessionModule extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), GuiceProbe.class)
                .addBinding()
                .to(TestingSessionProbe.class);

            bind(Session.class)
                .annotatedWith(Names.named("cache"))
                .to(TestingSession.class);
            bind(Session.class)
                .to(TestingSession.class);
            Multibinder.newSetBinder(binder(), CassandraModule.class).addBinding().toInstance(CassandraBlobCacheModule.MODULE);
        }

        @Provides
        @Singleton
        TestingSession provideSession(SessionWithInitializedTablesFactory factory) {
            return new TestingSession(factory.get());
        }
    }

    @RegisterExtension
    static JamesServerExtension jamesServerExtension = WithCacheImmutableTest.baseExtensionBuilder()
        .lifeCycle(JamesServerExtension.Lifecycle.PER_TEST)
        .overrideServerModule(new TestingSessionModule())
        .build();

    @Test
    void receivingAMailShouldNotPerformAnyLWT(GuiceJamesServer server) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(Pop3ServerContract.DOMAIN)
            .addUser(Pop3ServerContract.USER, PASSWORD);
        server.getProbe(MailboxProbeImpl.class)
            .createMailbox(MailboxPath.inbox(Username.of(Pop3ServerContract.USER)));

        // Given session recording is turned on
        StatementRecorder statementRecorder = new StatementRecorder();
        server.getProbe(TestingSessionProbe.class).getTestingSession()
            .recordStatements(statementRecorder);

        // When an email is received
        SMTPMessageSender smtpMessageSender = new SMTPMessageSender(JMAPTestingConstants.DOMAIN);
        smtpMessageSender.connect("127.0.0.1", server.getProbe(SmtpGuiceProbe.class).getSmtpPort());
        smtpMessageSender.sendMessage("bob@" + JMAPTestingConstants.DOMAIN, Pop3ServerContract.USER);
        Thread.sleep(100);
        Awaitility.await().untilAsserted(() -> assertThat(server.getProbe(SpoolerProbe.class).processingFinished()).isTrue());

        // Then no LWT was executed
        assertThat(statementRecorder.listExecutedStatements(StatementRecorder.Selector.preparedStatementContaining(" IF ")))
            .isEmpty();
    }
}