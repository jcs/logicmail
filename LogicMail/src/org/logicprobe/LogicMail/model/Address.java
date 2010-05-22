/*-
 * Copyright (c) 2009, Derek Konigsberg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.util.StringParser;

/**
 * Represents an address in an E-Mail message.
 */
public class Address {
	private final String address;
	private final String name;
	
	/**
	 * Instantiates a new address.
	 * 
	 * @param address Address string, such as "doej@generic.org".
	 * @param name Address display name, such as "John Doe".
	 */
	public Address(String address, String name) {
		//TODO: Add validation and/or cleanup of input strings
		this.address = address;
		this.name = name;
	}
	
	/**
	 * Instantiates a new address
	 * 
	 * @param completeAddress Complete address string, such as "John Doe <doej@generic.org>".
	 */
	public Address(String completeAddress) {
		String[] recipient = StringParser.parseRecipient(completeAddress);
		this.name = recipient[0];
		this.address = recipient[1];
	}
	
	/**
	 * Gets the address string.
	 * 
	 * @return the address
	 */
	public String getAddress() {
		return this.address;
	}
	
	/**
	 * Gets the display name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String result;
		if(this.name == null) {
			result = this.address;
		}
		else {
			StringBuffer buf = new StringBuffer();
			buf.append('\"');
			buf.append(this.name);
			buf.append("\" <");
			buf.append(this.address);
			buf.append('>');
			result = buf.toString();
		}
		return result;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((address == null) ? 0 : address.hashCode());
        result = 31 * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Address other = (Address) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
