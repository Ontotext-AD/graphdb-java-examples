package com.ontotext.graphdb;

import com.ontotext.test.base.Await;
import com.ontotext.test.replicationcluster.base.*;
import com.ontotext.test.replicationcluster.base.api.Master;
import com.ontotext.test.replicationcluster.base.api.WireMockMaster;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.concurrent.TimeUnit;

public class WorkerOutOfSyncTest {

    EmbeddedClusterBuilder builder = new EmbeddedClusterBuilder()
            .withWiremock() // use a wiremock for the between nodes communication
            ;

    /**
     * Create the nodes.
     */
    private final WorkerNode workerNode1 = builder.newWorker();
    private final WorkerNode workerNode2 = builder.newWorker();
    private final WorkerNode workerNode3 = builder.newWorker();
    private final Master masterNode = builder.newMaster();

    // declare the rulechain that will start the nodes and will stop them for you in the end of the test
    @Rule
    public RuleChain ruleChain = builder.getRuleChain();

    @Test
    public void workerOutOfSync() throws Exception {
        masterNode.addWorker(workerNode1);
        masterNode.addWorker(workerNode2);
        masterNode.addWorker(workerNode3);

        masterNode.awaitWorkersAreReadable(20);


        insertSimpleData();

        //Go out of synch. Execute update directly on worker in order to
	    //desynchronized it with the master.
        workerNode1.executeUpdateDirectly();

        insertSimpleData();

        //confirm that worker1 is in state OUT_OF_SYNC
        Await.await(5, TimeUnit.SECONDS)
                .until(WorkerStateChanged.outOfSyncForced((WireMockMaster) masterNode, workerNode1.getRepositoryUrl()));

        masterNode.awaitWorkersAreReadable(20);

    }

    private void insertSimpleData() {
        try (RepositoryConnection connection = masterNode.getConnection()) {
            connection.begin();
            final ValueFactory vf = connection.getValueFactory();
            connection.add(vf.createIRI("u:a"), vf.createIRI("u:p"), vf.createIRI("u:b"));

            connection.commit();
        }
    }
}
