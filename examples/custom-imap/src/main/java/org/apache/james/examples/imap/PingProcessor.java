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

package org.apache.james.examples.imap;

import javax.inject.Inject;

import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.processor.base.AbstractProcessor;
import org.apache.james.util.MDCBuilder;

import reactor.core.publisher.Mono;

public class PingProcessor extends AbstractProcessor<PingImapPackages.PingRequest> {

    private final StatusResponseFactory factory;

    @Inject
    public PingProcessor(StatusResponseFactory factory) {
        super(PingImapPackages.PingRequest.class);
        this.factory = factory;
    }

    @Override
    protected Mono<Void> doProcess(PingImapPackages.PingRequest request, Responder responder, ImapSession session) {
        return Mono.fromRunnable(() -> responder.respond(new PingImapPackages.PingResponse()))
            .then(Mono.fromRunnable(() -> responder.respond(factory.taggedOk(request.getTag(), request.getCommand(), HumanReadableText.COMPLETED))));
    }

    @Override
    protected MDCBuilder mdc(PingImapPackages.PingRequest message) {
        return MDCBuilder.create()
            .addToContext(MDCBuilder.ACTION, "PING");
    }
}