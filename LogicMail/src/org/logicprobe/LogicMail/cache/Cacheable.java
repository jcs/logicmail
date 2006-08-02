/*
 * Cacheable.java
 *
 * Created on August 1, 2006, 5:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.logicprobe.LogicMail.cache;

/**
 * Interface for data classes that are saveable
 * to the cache.
 */
public interface Cacheable {
    public byte[] serialize();
    public void deserialize(byte[] byteArray);
}
