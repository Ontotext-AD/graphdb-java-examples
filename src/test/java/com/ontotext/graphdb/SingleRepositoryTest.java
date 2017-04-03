package com.ontotext.graphdb;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.ontotext.test.JMXServerRule;
import com.ontotext.test.TemporaryLocalFolder;
import com.ontotext.test.base.RepositoryRule;
import com.ontotext.test.utils.EnterpriseUtils;

/**
 * A test that will run on a single standard edition node or
 * in enterprise - a master and a single worker.
 */
public class SingleRepositoryTest {

	public TemporaryLocalFolder tmpFolder = new TemporaryLocalFolder();

	public RepositoryRule repositoryRule = new RepositoryRule(tmpFolder) {
		@Override
		protected RepositoryConfig createRepositoryConfiguration() {
		/* there is also StandardUtils.createOwlimSe(ruluset); for enterprise edition tests */
			return EnterpriseUtils.createOwlimEnt(/* ruleset */"owl-horst-optimized");
		}

		@Override
		protected boolean useRemoteRepositoryManager() {
			// should we run a full blown http instance or use an embedded repository. For the http instance you will
			// need to extract the distribution first, see pom.xml for more info.
			return super.useRemoteRepositoryManager(); // false by default
		}


		protected boolean useCluster() {
			// should we use a master single worker configuration if we are in enterprise mode
			// to test the code in a "cluster"
			return super.useCluster(); // false by default
		}

		protected boolean clearRepositoryAfterEachTest() {
			// should we clear the repository after each test
			return super.clearRepositoryAfterEachTest(); // true by default
		}

		protected void oneTimeSetup() throws Exception {
			// logic that should be executed on the first repository/cluster creation
		}
	};

	/**
	 * Rule chain that we make sure that we start and stop a graphdb server before each test
	 */
	@Rule
	public RuleChain ruleChain = RuleChain.outerRule(tmpFolder)
			.around(JMXServerRule.system()) // this will start the jmx server so we can connect the master to the worker in cluster mode
			.around(this.repositoryRule);


	@Test
	public void testSize() throws Exception {
		final Repository repository = repositoryRule.getRepository(); // this will return local sail repository or http repository
		try (RepositoryConnection connection = repository.getConnection()) {
			Assert.assertEquals(0, connection.size());
		}
	}
}
