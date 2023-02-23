package cn.polarismesh.polaris.sync.common.database;

import org.postgresql.ds.PGPoolingDataSource;

public class PGSingleton {

    private static DatabaseOperator INSTANCE;

    public static void init(String url, String user, String password) {
        PGPoolingDataSource source = new PGPoolingDataSource();
        source.setDataSourceName("kong-postgresql");
        source.setURL(url);
        source.setUser(user);
        source.setPassword(password);
        source.setStringType("unspecified");
        source.setMaxConnections(200);
        INSTANCE = new DatabaseOperator(source);
    }

    public static  DatabaseOperator getINSTANCE() {
        return INSTANCE;
    }


}
