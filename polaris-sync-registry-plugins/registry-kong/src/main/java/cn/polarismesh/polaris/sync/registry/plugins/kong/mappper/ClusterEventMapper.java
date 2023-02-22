package cn.polarismesh.polaris.sync.registry.plugins.kong.mappper;

import cn.polarismesh.polaris.sync.common.database.RecordSupplier;
import cn.polarismesh.polaris.sync.registry.plugins.kong.model.ClusterEventObject;
import cn.polarismesh.polaris.sync.registry.plugins.kong.model.TargetObject;

import java.sql.ResultSet;

public class ClusterEventMapper implements RecordSupplier<ClusterEventObject> {

    @Override
    public String getQueryListSqlTemplate(boolean first) {
        return null;
    }

    @Override
    public String getQueryOneSqlTemplate() {
        return null;
    }

    @Override
    public String getInsertOneSqlTemplate() {
        return "insert into cluster_events(id,node_id,at,expire_at,channel,data) values(?,?,?,?,?,?);";
    }

    @Override
    public ClusterEventObject apply(ResultSet t) throws Exception {
        return null;
    }

    @Override
    public ClusterEventObject merge(ClusterEventObject cur, ClusterEventObject pre) {
        return null;
    }
}
