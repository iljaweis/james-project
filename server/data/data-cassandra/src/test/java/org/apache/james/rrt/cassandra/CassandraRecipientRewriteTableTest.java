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

package org.apache.james.rrt.cassandra;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;
import org.apache.james.rrt.lib.RecipientRewriteTableContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraRecipientRewriteTableTest implements RecipientRewriteTableContract {
    static final CassandraModule MODULE = CassandraModule.aggregateModules(
        CassandraRRTModule.MODULE,
        CassandraSchemaVersionModule.MODULE);

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(MODULE);

    AbstractRecipientRewriteTable recipientRewriteTable;
    CassandraRecipientRewriteTableDAO recipientRewriteTableDAO;
    CassandraMappingsSourcesDAO mappingsSourcesDAO;
    CassandraSchemaVersionDAO cassandraSchemaVersionDAO;

    @BeforeEach
    void setup(CassandraCluster cassandra) throws Exception {
        cassandraSchemaVersionDAO = new CassandraSchemaVersionDAO(cassandra.getConf());
        recipientRewriteTableDAO = new CassandraRecipientRewriteTableDAO(cassandra.getConf());
        mappingsSourcesDAO = new CassandraMappingsSourcesDAO(cassandra.getConf());

        setUp();
    }

    @AfterEach
    void teardown() throws Exception {
        tearDown();
    }

    @Override
    public void createRecipientRewriteTable() {
        recipientRewriteTable = new CassandraRecipientRewriteTable(recipientRewriteTableDAO, mappingsSourcesDAO);
    }

    @Override
    public AbstractRecipientRewriteTable virtualUserTable() {
        return recipientRewriteTable;
    }
}
