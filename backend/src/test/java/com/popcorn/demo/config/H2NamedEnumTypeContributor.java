package com.popcorn.demo.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

public class H2NamedEnumTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		String dialectName = serviceRegistry.getService(JdbcServices.class)
				.getDialect()
				.getClass()
				.getName();
		if (!dialectName.contains("H2Dialect")) {
			return;
		}
		JdbcTypeRegistry registry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		registry.addDescriptor(SqlTypes.NAMED_ENUM, VarcharJdbcType.INSTANCE);
	}
}
