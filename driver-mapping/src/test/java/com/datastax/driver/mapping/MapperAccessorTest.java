package com.datastax.driver.mapping;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.driver.core.CCMBridge;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

public class MapperAccessorTest extends CCMBridge.PerClassSingleNodeCluster {

    @Override protected Collection<String> getTableDefinitions() {
        return Lists.newArrayList("CREATE TABLE foo (i int primary key)");
    }

    @Test(groups = "short")
    public void should_implement_toString() {
        SystemAccessor accessor = new MappingManager(session)
            .createAccessor(SystemAccessor.class);

        assertThat(accessor.toString())
            .isEqualTo("SystemAccessor implementation generated by the Cassandra driver mapper");
    }

    @Test(groups = "short")
    public void should_implement_equals_and_hashCode() {
        SystemAccessor accessor = new MappingManager(session)
            .createAccessor(SystemAccessor.class);

        assertThat(accessor).isNotEqualTo(new Object());
        assertThat(accessor.hashCode()).isEqualTo(System.identityHashCode(accessor));
    }

    @Accessor
    public interface SystemAccessor {
        @Query("select release_version from system.local")
        ResultSet getCassandraVersion();
    }
}
