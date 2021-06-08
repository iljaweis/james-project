package org.apache.james.pop3.webadmin;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.pop3server.mailbox.task.MetaDataFixInconsistenciesService;
import org.apache.james.pop3server.mailbox.task.MetaDataFixInconsistenciesService.RunningOptions;
import org.apache.james.pop3server.mailbox.task.MetaDataFixInconsistenciesTask;
import org.apache.james.task.Task;
import org.apache.james.webadmin.tasks.TaskFromRequestRegistry.TaskRegistration;
import org.apache.james.webadmin.tasks.TaskRegistrationKey;

import com.google.common.base.Preconditions;

import spark.Request;

public class Pop3MetaDataFixInconsistenciesTaskRegistration extends TaskRegistration {
    private static final TaskRegistrationKey REGISTRATION_KEY = TaskRegistrationKey.of("fixPop3Inconsistencies");

    @Inject
    public Pop3MetaDataFixInconsistenciesTaskRegistration(MetaDataFixInconsistenciesService fixInconsistenciesService) {
        super(REGISTRATION_KEY, request -> fixInconsistencies(fixInconsistenciesService, request));
    }

    private static Task fixInconsistencies(MetaDataFixInconsistenciesService fixInconsistenciesService, Request request) {
        RunningOptions runningOptions = getMessagesPerSecond(request)
            .map(RunningOptions::withMessageRatePerSecond)
            .orElse(RunningOptions.DEFAULT);
        return new MetaDataFixInconsistenciesTask(fixInconsistenciesService, runningOptions);
    }

    private static Optional<Integer> getMessagesPerSecond(Request req) {
        try {
            return Optional.ofNullable(req.queryParams("messagesPerSecond"))
                .map(Integer::parseInt)
                .map(msgPerSeconds -> {
                    Preconditions.checkArgument(msgPerSeconds > 0, "'messagesPerSecond' must be strictly positive");
                    return msgPerSeconds;
                });
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("'messagesPerSecond' must be numeric");
        }
    }
}
