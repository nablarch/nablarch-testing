package nablarch.test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import nablarch.core.repository.SystemRepository;

import org.junit.Test;

/**
 * @author T.Kawasaki
 */
public class RepositoryInitializerTest {
    @Test
    public void testInitializeDefaultRepository() {
        RepositoryInitializer.initializeDefaultRepository();
        assertNotNull(SystemRepository.getString("defaultLocale"));
        assertEquals("ja_JP", SystemRepository.getString("defaultLocale"));
        SystemRepository.clear();
        assertNull(SystemRepository.getString("defaultLocale"));
        RepositoryInitializer.initializeDefaultRepository();
        assertEquals("ja_JP", SystemRepository.getString("defaultLocale"));
    }

    @Test
    public void testReInitializeRepository() {
        assertNull(SystemRepository.getString("RepositoryInitializerTest"));
        assertNull(SystemRepository.getString("RepositoryInitializerTest2"));
        try {
            RepositoryInitializer.reInitializeRepository(
                    "nablarch/test/RepositoryInitializerTest.xml",
                    "nablarch/test/RepositoryInitializerTest2.xml");
            assertEquals("someValue", SystemRepository.getString("RepositoryInitializerTest"));
            assertEquals("someValue", SystemRepository.getString("RepositoryInitializerTest2"));
        } finally {
            RepositoryInitializer.revertDefaultRepository();
        }

        assertNull(SystemRepository.getString("RepositoryInitializerTest"));
        try {
            RepositoryInitializer.reInitializeRepository("nablarch/test/RepositoryInitializerTest.xml");
            assertEquals("someValue", SystemRepository.getString("RepositoryInitializerTest"));
            RepositoryInitializer target = new RepositoryInitializer();
            target.afterTestClass();
        } finally {
            RepositoryInitializer.revertDefaultRepository();
        }
    }

    @Test
    public void testRecreateRepository() {
        assertNull(SystemRepository.getString("RepositoryInitializerTest"));
        try {
            RepositoryInitializer.recreateRepository("nablarch/test/RepositoryInitializerTest.xml");
            assertEquals("someValue", SystemRepository.getString("RepositoryInitializerTest"));
        } finally {
            RepositoryInitializer.revertDefaultRepository();
        }
    }


    @Test
    public void testReInitializeRepositoryFail() {

        try {
            RepositoryInitializer.reInitializeRepository("nablarch/test/RepositoryInitializerTestFail.xml");
            fail();
        } catch (RuntimeException actual) {
            assertThat(actual.getMessage(), containsString("failed reinitializing repository"));
        } finally {
            RepositoryInitializer.revertDefaultRepository();
        }
    }

    @Test
    public void testRecreateRepositoryFail() {
        try {
            RepositoryInitializer.recreateRepository("nablarch/test/RepositoryInitializerTestFail.xml");
            fail();
        } catch (RuntimeException actual) {
            assertThat(actual.getMessage(), containsString("failed reinitializing repository"));
        } finally {
            RepositoryInitializer.revertDefaultRepository();
        }
    }
}
