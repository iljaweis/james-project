package org.apache.james;

import static org.apache.james.webadmin.routes.MailboxesRoutes.ALL_MAILBOXES_TASKS;

import org.apache.james.pop3.webadmin.Pop3MetaDataFixInconsistenciesTaskRegistration;
import org.apache.james.webadmin.tasks.TaskFromRequestRegistry;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class Pop3FixInconsistenciesWebAdminModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new Pop3FixInconsistenciesTaskSerializationModule());

        Multibinder.newSetBinder(binder(), TaskFromRequestRegistry.TaskRegistration.class, Names.named(ALL_MAILBOXES_TASKS))
            .addBinding()
            .to(Pop3MetaDataFixInconsistenciesTaskRegistration.class);
    }
}
