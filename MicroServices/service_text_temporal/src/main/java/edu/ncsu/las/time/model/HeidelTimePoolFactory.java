package edu.ncsu.las.time.model;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;;

public class HeidelTimePoolFactory extends BasePooledObjectFactory<HeidelTime> {

	@Override
	public HeidelTime create() throws Exception {
		return new HeidelTime();
	}

	@Override
	public PooledObject<HeidelTime> wrap(HeidelTime ht) {
		return new DefaultPooledObject<HeidelTime>(ht);
	}

}
