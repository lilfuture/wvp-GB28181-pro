package com.genersoft.iot.vmp.conf;

import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 配置mybatis
 */
@Configuration
@Order(value=1)
public class MybatisConfig {

    private static final Logger log = LoggerFactory.getLogger(MybatisConfig.class);

    @Autowired
    private UserSetting userSetting;

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("DB2", "db2");
        properties.setProperty("Derby", "derby");
        properties.setProperty("H2", "h2");
        properties.setProperty("HSQL", "hsql");
        properties.setProperty("Informix", "informix");
        properties.setProperty("MS-SQL", "ms-sql");
        properties.setProperty("PostgreSQL", "postgresql");
        properties.setProperty("Sybase", "sybase");
        properties.setProperty("Hana", "hana");
        properties.setProperty("DM", "dm");
        properties.setProperty("KingbaseES", "kingbase");
        properties.setProperty("KingBase8", "kingbase");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, DatabaseIdProvider databaseIdProvider) throws Exception {
       final SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        if (userSetting.getSqlLog()){
            config.setLogImpl(StdOutImpl.class);
        }
        config.setMapUnderscoreToCamelCase(true);
        String databaseId = databaseIdProvider.getDatabaseId(dataSource);
        if (databaseId == null) {
            log.warn("[MyBatis] 未能从数据源识别数据库类型, 回退为 mysql");
            databaseId = "mysql";
        }
        config.setDatabaseId(databaseId);
        sqlSessionFactory.setConfiguration(config);
        // 注意: 不可调用 setDatabaseIdProvider, SqlSessionFactoryBean 构建时会用其返回值覆盖 Configuration.databaseId,
        // 识别失败时返回 null 会覆盖上面的兜底值, 导致 Mapper 无法匹配 databaseId 注解
        SqlSessionFactory factory = sqlSessionFactory.getObject();
        log.info("[MyBatis] 数据源识别 databaseId={}, 生效 databaseId={}",
                databaseId, factory.getConfiguration().getDatabaseId());
        return factory;
    }

}
