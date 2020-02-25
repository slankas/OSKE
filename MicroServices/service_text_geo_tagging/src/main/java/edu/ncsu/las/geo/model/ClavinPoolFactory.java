package edu.ncsu.las.geo.model;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;;

public class ClavinPoolFactory extends BasePooledObjectFactory<Clavin> {

	@Override
	public Clavin create() throws Exception {
		return new Clavin();
	}

	@Override
	public PooledObject<Clavin> wrap(Clavin ht) {
		return new DefaultPooledObject<Clavin>(ht);
	}

}
