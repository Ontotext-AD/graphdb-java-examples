package com.ontotext.graphdb;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.ontotext.test.replicationcluster.base.EmbeddedClusterBuilder;
import com.ontotext.test.replicationcluster.base.MasterNode;
import com.ontotext.test.replicationcluster.base.WorkerNode;

/**
 * @author Nikola Petrov nikola.petrov@ontotext.com
 */
public class ClusterSetupTest {

	EmbeddedClusterBuilder builder = new EmbeddedClusterBuilder()
			.withWiremock() // use a wiremock for the between nodes communication
			;

	/**
	 * Create the nodes.
	 */
	private final WorkerNode workerNode1 = builder.newWorker();
	private final WorkerNode workerNode2 = builder.newWorker();
	private final WorkerNode workerNode3 = builder.newWorker();
	private final MasterNode masterNode = builder.newMaster();

	// declare the rulechain that will start the nodes and will stop them for you in the end of the test
	@Rule
	public RuleChain ruleChain = builder.getRuleChain();

	@Test
	public void shouldProperlyJoinNewWorker() throws Exception {
		masterNode.addWorker(workerNode1);
		masterNode.addWorker(workerNode2);
		masterNode.addWorker(workerNode3);

		// the new worker is not started, we have to start it explicitly because it was
		// declared after the rulechain was initialized
		final WorkerNode workerNode = builder.newWorker();
		workerNode.start();
		// the worker is empty
		Assert.assertEquals(0, workerNode.size());

		// add the new worker to the cluster and execute an update
		masterNode.addWorker(workerNode);
		masterNode.awaitWorkersAreReadable(10);
		try (RepositoryConnection connection = masterNode.getConnection()) {
			final ValueFactory vf = connection.getValueFactory();
			connection.add(vf.createIRI("u:a"), vf.createIRI("u:p"), vf.createIRI("u:b"));
		}
		masterNode.awaitNoPendingWrites(5);
		
		// verify that the worker run the update successfully
		Assert.assertEquals(1, workerNode.size());

		// note that we don't need to stop the worker, the builder's rulechain will that for us
	}

}
