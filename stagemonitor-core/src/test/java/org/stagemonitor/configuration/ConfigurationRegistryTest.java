package org.stagemonitor.configuration;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.CorePlugin;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.Stagemonitor.STAGEMONITOR_PASSWORD;

public class ConfigurationRegistryTest {

	private ConfigurationRegistry configuration;
	private CorePlugin corePlugin;

	@Before
	public void init() {
		configuration = new ConfigurationRegistry(Collections.singletonList(new CorePlugin()),
				Collections.<ConfigurationSource>singletonList(new SimpleSource()), STAGEMONITOR_PASSWORD);
		corePlugin = configuration.getConfig(CorePlugin.class);
	}

	@Test
	public void testGetConfigSubclass() {
		final CorePlugin corePluginMock = mock(CorePlugin.class);
		configuration = new ConfigurationRegistry(Collections.singletonList(corePluginMock),
				Collections.<ConfigurationSource>singletonList(new SimpleSource()), STAGEMONITOR_PASSWORD);
		assertSame(corePluginMock, configuration.getConfig(CorePlugin.class));
		assertNull(configuration.getConfig(new ConfigurationOptionProvider(){}.getClass()));
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException {
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("'stagemonitor.password' is not set.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWrongDatatype() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "").add("stagemonitor.internal.monitoring", "1"));
		configuration.reloadAllConfigurationOptions();
		assertFalse(corePlugin.isInternalMonitoringActive());

		assertEquals("Error in Test Configuration Source: Can't convert '1' to Boolean.", configuration.getConfigurationOptionByKey("stagemonitor.internal.monitoring").getErrorMessage());

		configuration.save("stagemonitor.internal.monitoring", "true", "Test Configuration Source", null);
		assertTrue(corePlugin.isInternalMonitoringActive());

		assertNull(configuration.getConfigurationOptionByKey("stagemonitor.internal.monitoring").getErrorMessage());

	}

	@Test
	public void testUpdateConfigurationWrongConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "foo", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Configuration source 'foo' does not exist.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNotSaveableConfigurationSource() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		configuration.addConfigurationSource(new SystemPropertyConfigurationSource());
		assertFalse(corePlugin.isInternalMonitoringActive());
		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Java System Properties", null);
			fail();
		} catch (UnsupportedOperationException e) {
			assertEquals("Saving to Java System Properties is not possible.", e.getMessage());
		}
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamicTransient() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertEquals(0, corePlugin.getConsoleReportingInterval());
		try {
			configuration.save("stagemonitor.reporting.interval.console", "1", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Non dynamic options can't be saved to a transient configuration source.", e.getMessage());
		}
		assertEquals(0, corePlugin.getConsoleReportingInterval());
	}

	@Test
	public void testUpdateConfigurationNonDynamicPersistent() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		final ConfigurationSource persistentSourceMock = mock(ConfigurationSource.class);
		when(persistentSourceMock.isSavingPossible()).thenReturn(true);
		when(persistentSourceMock.isSavingPersistent()).thenReturn(true);
		when(persistentSourceMock.getName()).thenReturn("Test Persistent");
		configuration.addConfigurationSource(persistentSourceMock);
		assertEquals(0, corePlugin.getConsoleReportingInterval());

		configuration.save("stagemonitor.reporting.interval.console", "1", "Test Persistent", null);

		verify(persistentSourceMock).save("stagemonitor.reporting.interval.console", "1");
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		try {
			configuration.save(STAGEMONITOR_PASSWORD, "pwd", "Transient Configuration Source", null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Config key 'stagemonitor.password' does not exist.", e.getMessage());
		}
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		try {
			configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Wrong password for 'stagemonitor.password'.", e.getMessage());
		}

		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		configuration.save("stagemonitor.internal.monitoring", "true", "Transient Configuration Source", "pwd");

		assertTrue(corePlugin.isInternalMonitoringActive());
	}
}
