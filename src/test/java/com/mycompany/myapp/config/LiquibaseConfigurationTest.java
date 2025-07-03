package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executor;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.env.Environment;
import tech.jhipster.config.JHipsterConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquibaseConfigurationTest {

    @Mock
    private Environment environment;

    @Mock
    private Executor executor;

    @Mock
    private LiquibaseProperties liquibaseProperties;

    @Mock
    private ObjectProvider<DataSource> liquibaseDataSource;

    @Mock
    private ObjectProvider<DataSource> dataSource;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.Liquibase liquibaseConfig;

    @Mock
    private DataSourceProperties dataSourceProperties;

    private LiquibaseConfiguration liquibaseConfiguration;

    @BeforeEach
    void setUp() {
        liquibaseConfiguration = new LiquibaseConfiguration(environment);
    }

    @Test
    void testCreateLiquibaseWithSyncStart() {
        // Given
        when(applicationProperties.getLiquibase()).thenReturn(liquibaseConfig);
        when(liquibaseConfig.getAsyncStart()).thenReturn(false);
        when(liquibaseProperties.getContexts()).thenReturn(List.of("!test"));
        when(liquibaseProperties.getDefaultSchema()).thenReturn("public");
        when(liquibaseProperties.getLiquibaseSchema()).thenReturn("public");
        when(liquibaseProperties.getLiquibaseTablespace()).thenReturn("users");
        when(liquibaseProperties.getDatabaseChangeLogTable()).thenReturn("DATABASECHANGELOG");
        when(liquibaseProperties.getDatabaseChangeLogLockTable()).thenReturn("DATABASECHANGELOGLOCK");
        when(liquibaseProperties.isDropFirst()).thenReturn(false);
        when(liquibaseProperties.isEnabled()).thenReturn(true);
        when(liquibaseDataSource.getIfAvailable()).thenReturn(null);
        when(dataSource.getIfUnique()).thenReturn(mock(DataSource.class));
        when(environment.matchesProfiles(JHipsterConstants.SPRING_PROFILE_NO_LIQUIBASE)).thenReturn(false);

        // When
        SpringLiquibase liquibase = liquibaseConfiguration.liquibase(
            executor,
            liquibaseProperties,
            liquibaseDataSource,
            dataSource,
            applicationProperties,
            dataSourceProperties
        );

        // Then
        assertThat(liquibase).isNotNull();
        assertThat(liquibase.getChangeLog()).isEqualTo("classpath:config/liquibase/master.xml");
        assertThat(liquibase.getContexts()).isEqualTo("!test");
        assertThat(liquibase.getDefaultSchema()).isEqualTo("public");
        assertThat(liquibase.getLiquibaseSchema()).isEqualTo("public");
        assertThat(liquibase.getLiquibaseTablespace()).isEqualTo("users");
        assertThat(liquibase.getDatabaseChangeLogTable()).isEqualTo("DATABASECHANGELOG");
        assertThat(liquibase.getDatabaseChangeLogLockTable()).isEqualTo("DATABASECHANGELOGLOCK");
        assertThat(liquibase.isDropFirst()).isFalse();
    }

    @Test
    void testCreateLiquibaseWithAsyncStart() {
        // Given
        when(applicationProperties.getLiquibase()).thenReturn(liquibaseConfig);
        when(liquibaseConfig.getAsyncStart()).thenReturn(true);
        when(liquibaseProperties.getContexts()).thenReturn(null);
        when(liquibaseProperties.getDefaultSchema()).thenReturn(null);
        when(liquibaseProperties.getLiquibaseSchema()).thenReturn(null);
        when(liquibaseProperties.getLiquibaseTablespace()).thenReturn(null);
        when(liquibaseProperties.getDatabaseChangeLogTable()).thenReturn(null);
        when(liquibaseProperties.getDatabaseChangeLogLockTable()).thenReturn(null);
        when(liquibaseProperties.isDropFirst()).thenReturn(false);
        when(liquibaseProperties.isEnabled()).thenReturn(true);
        when(liquibaseDataSource.getIfAvailable()).thenReturn(null);
        when(dataSource.getIfUnique()).thenReturn(mock(DataSource.class));
        when(environment.matchesProfiles(JHipsterConstants.SPRING_PROFILE_NO_LIQUIBASE)).thenReturn(false);

        // When
        SpringLiquibase liquibase = liquibaseConfiguration.liquibase(
            executor,
            liquibaseProperties,
            liquibaseDataSource,
            dataSource,
            applicationProperties,
            dataSourceProperties
        );

        // Then
        assertThat(liquibase).isNotNull();
        assertThat(liquibase.getChangeLog()).isEqualTo("classpath:config/liquibase/master.xml");
    }

    @Test
    void testCreateLiquibaseWithNoLiquibaseProfile() {
        // Given
        when(applicationProperties.getLiquibase()).thenReturn(liquibaseConfig);
        when(liquibaseConfig.getAsyncStart()).thenReturn(false);
        when(liquibaseProperties.getContexts()).thenReturn(null);
        when(liquibaseProperties.isEnabled()).thenReturn(true);
        when(liquibaseDataSource.getIfAvailable()).thenReturn(null);
        when(dataSource.getIfUnique()).thenReturn(mock(DataSource.class));
        when(environment.matchesProfiles(JHipsterConstants.SPRING_PROFILE_NO_LIQUIBASE)).thenReturn(true);

        // When
        SpringLiquibase liquibase = liquibaseConfiguration.liquibase(
            executor,
            liquibaseProperties,
            liquibaseDataSource,
            dataSource,
            applicationProperties,
            dataSourceProperties
        );

        // Then
        assertThat(liquibase).isNotNull();
    }

    @Test
    void testConstructor() {
        // When
        LiquibaseConfiguration config = new LiquibaseConfiguration(environment);

        // Then
        assertThat(config).isNotNull();
    }
}
