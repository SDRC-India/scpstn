package org.sdrc.scpstn.collect.android.listeners;

import java.util.HashMap;

/**
 * 
 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
 *
 */
public interface UpdateListener {
	
	void updateOperationComplete(HashMap<Integer, String> result);
}
