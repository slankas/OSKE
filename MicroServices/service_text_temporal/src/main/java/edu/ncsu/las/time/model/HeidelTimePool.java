package edu.ncsu.las.time.model;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class HeidelTimePool  extends GenericObjectPool<HeidelTime> {

	public HeidelTimePool(PooledObjectFactory<HeidelTime> factory) {
		super(factory);
	}

    public HeidelTimePool(PooledObjectFactory<HeidelTime> factory, GenericObjectPoolConfig config) {
        super(factory, config);
    }
}
