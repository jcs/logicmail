/*-
 * Copyright (c) 2008, Derek Konigsberg
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

package org.logicprobe.LogicMail.util;

import java.util.NoSuchElementException;

public class Queue {
	private static class Node {
		public Object item;
		public Node next;
	}
	
	private Node head;
	private Node tail;
	
	public Queue() {
		head = null;
		tail = null;
	}
	
	/**
	 * Adds an element to the queue.
	 * @param element The element
	 * @throws NullPointerException if the item is null
	 */
	public void add(Object element) {
		if(element == null) {
			throw new NullPointerException();
		}
		Node node = new Node();
		node.item = element;
		node.next = null;
		
		if(head == null) {
			head = node;
			tail = head;
		}
		else {
			tail.next = node;
			tail = tail.next;
		}
	}
	
	/**
	 * Retrieves the element at the head of the queue.
	 * @return The element, or null if the queue is empty
	 */
	public Object element() {
		if(head == null) {
			return null;
		}
		else {
			return head.item;
		}
	}
	
	/**
	 * Retrieves and removes the element at the head of the queue.
	 * @return The element
	 * @throws NoSuchElementException if the queue is empty
	 */
	public Object remove() {
		if(head == null) {
			throw new NoSuchElementException();
		}
		Object item = head.item;
		head.item = null;
		head = head.next;
		return item;
	}
	
	/**
	 * Removes all elements from the queue.
	 */
	public void clear() {
		while(head != null) {
			Node temp = head.next;
			head.item = null;
			head.next = null;
			head = temp;
		}
		tail = null;
	}
}
