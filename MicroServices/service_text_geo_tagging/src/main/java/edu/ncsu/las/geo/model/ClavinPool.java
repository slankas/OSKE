package edu.ncsu.las.geo.model;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ClavinPool  extends GenericObjectPool<Clavin> {

	public ClavinPool(PooledObjectFactory<Clavin> factory) {
		super(factory);
	}

    public ClavinPool(PooledObjectFactory<Clavin> factory, GenericObjectPoolConfig config) {
        super(factory, config);
    }
}
