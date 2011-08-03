/*-
 * Copyright (c) 2011, Derek Konigsberg
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
package org.logicprobe.LogicMail.mail.imap;

import java.util.Vector;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.mail.imap.ImapProtocol.SelectResponse;

import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.SimpleSortingIntVector;

/**
 * Encapsulates all state information for an IMAP mailbox, for data that does
 * not need to be exposed outside the IMAP client implementation.
 */
public class MailboxState {
    private final Object lock = new Object();
    private final IntHashtable indexToTokenMap;
    private final SimpleSortingIntVector indexVector;
    private int exists;
    private int recent;
    private int unseen;
    private int uidNext = -1;
    private int uidValidity;
    
    public MailboxState() {
        indexToTokenMap = new IntHashtable();
        indexVector = new SimpleSortingIntVector();
        indexVector.setSortAsAdded(SimpleSortingIntVector.SORT_TYPE_NUMERIC);
    }

    /**
     * Clear the current state for this mailbox.
     */
    public void clear() {
        synchronized(lock) {
            indexToTokenMap.clear();
            indexVector.removeAllElements();
            exists = 0;
            recent = 0;
            unseen = 0;
            uidNext = -1;
            uidValidity = 0;
        }
    }
    
    /**
     * Handle mailbox selection by populating the state with values from the
     * <tt>SELECT</tt> response.
     *
     * @param selectResponse the parsed select response
     * @return true, if the index map is still valid
     */
    public boolean mailboxSelected(ImapProtocol.SelectResponse selectResponse) {
        boolean result = true;
        synchronized(lock) {
            if(!indexMapStillValid(selectResponse)) {
                indexToTokenMap.clear();
                indexVector.removeAllElements();
                result = false;
            }
            this.exists = selectResponse.exists;
            this.recent = selectResponse.recent;
            this.unseen = selectResponse.unseen;
            this.uidNext = selectResponse.uidNext;
            this.uidValidity = selectResponse.uidValidity;
        }
        return result;
    }

    private boolean indexMapStillValid(SelectResponse selectResponse) {
        if(this.uidValidity != selectResponse.uidValidity) {
            // If UIDVALIDITY has changed, all bets are off
            return false;
        }
        else if(this.exists == selectResponse.exists
                && this.uidNext == selectResponse.uidNext) {
            // If EXISTS and UIDNEXT are unchanged, we are still valid
            return true;
        }
        else if(this.exists < selectResponse.exists && this.uidNext < selectResponse.uidNext
                && (selectResponse.exists - this.exists) == (selectResponse.uidNext - this.uidNext)) {
            // If EXISTS and UIDNEXT incremented by the same amount, we are still valid
            return true;
        }
        else {
            return false;
        }
    }

    public int getExists() {
        return exists;
    }
    
    public void setExists(int exists) {
        synchronized(lock) {
            this.exists = exists;
        }
    }

    public int getRecent() {
        return recent;
    }
    
    public void setRecent(int recent) {
        synchronized(lock) {
            this.recent = recent;
        }
    }
    
    public int getUnseen() {
        return unseen;
    }
    
    public void setUnseen(int unseen) {
        synchronized(lock) {
            this.unseen = unseen;
        }
    }
    
    public int getUidNext() {
        return uidNext;
    }

    public void setUidNext(int uidNext) {
        synchronized(lock) {
            this.uidNext = uidNext;
        }
    }
    
    public int getUidValidity() {
        return uidValidity;
    }

    public void setUidValidity(int uidValidity) {
        synchronized(lock) {
            this.uidValidity = uidValidity;
        }
    }
    
    /**
     * Update mailbox state from a <tt>FETCH</tt> response.
     *
     * @param token the token referencing the message
     */
    public void messageFetched(MessageToken token) {
        synchronized(lock) {
            int index = ((ImapMessageToken)token).getMessageIndex();
            if(indexToTokenMap.containsKey(index)) {
                if(token.equals(indexToTokenMap.get(index))) {
                    // Do nothing if this is a redundant update
                    return;
                }
                else
                {
                    // This means we missed an untagged expunge, or otherwise have
                    // bad mailbox state information.  This is a very dangerous
                    // situation where the only safe action is to log an error,
                    // completely flush our index-to-UID maps, and record this
                    // fetch as our only known good data.
    
                    EventLogger.logEvent(AppInfo.GUID,
                            ("Untagged FETCH response is out of sync with known mailbox state").getBytes(),
                            EventLogger.ERROR);
                    AnalyticsDataCollector.getInstance().onApplicationError(
                            "Untagged FETCH response is out of sync with known mailbox state");
                    
                    indexToTokenMap.clear();
                    indexVector.removeAllElements();
                    indexToTokenMap.put(index, token);
                    indexVector.addElement(index);
                }
            }
            indexToTokenMap.put(index, token);
            indexVector.addElement(index);
        }
    }
    
    /**
     * Gets the token for a message.
     *
     * @param index the index of the message within the mailbox
     * @return the message token, if available
     */
    public ImapMessageToken getMessageToken(int index) {
        ImapMessageToken result;
        synchronized(lock) {
            result = (ImapMessageToken)indexToTokenMap.get(index);
        }
        return result;
    }
    
    /**
     * Expunge a message, by index, from the mailbox.
     *
     * @param indices the indices of the expunged messages
     * @param updatedTokens empty <code>Vector</code> to be populated with
     *        <code>MessageToken</code>s updated by the expunge operation
     * @return tokens for the expunged messages, if available
     */
    public ImapMessageToken[] messagesExpunged(int[] indices, Vector updatedTokens) {
        ImapMessageToken[] result = new ImapMessageToken[indices.length];
        for(int i=0; i<indices.length; i++) {
            result[i] = messageExpunged(indices[i], updatedTokens);
        }
        return result;
    }
    
    /**
     * Expunge a message, by index, from the mailbox.
     *
     * @param index the index of the expunged message
     * @param updatedTokens empty <code>Vector</code> to be populated with
     *        <code>MessageToken</code>s updated by the expunge operation
     * @return token for the expunged message, if available
     */
    public ImapMessageToken messageExpunged(int index, Vector updatedTokens) {
        ImapMessageToken result;
        synchronized(lock) {
            result = (ImapMessageToken)indexToTokenMap.remove(index);
            if(result != null) {
                removeIndexFromVector(index, updatedTokens);
            }
            else {
                shiftVectorAroundRemovedIndex(index, updatedTokens);
            }
        }
        return result;
    }
    
    private void removeIndexFromVector(int index, Vector updatedTokens) {
        // Find the index of the expunged element
        int p = indexVector.binarySearch(index, SimpleSortingIntVector.SORT_TYPE_NUMERIC);
        
        // Remove the element from the vector
        indexVector.removeElementAt(p);

        // Get the new size of the vector
        int size = indexVector.size();
        
        // If we removed the last element, then return
        if(p == size) { return; }
        
        // Otherwise, iterate over the portion of the vector following the
        // removed element and decrement the index values
        decrementIndexValues(p, updatedTokens);
    }

    private void shiftVectorAroundRemovedIndex(int index, Vector updatedTokens) {
        int size = indexVector.size();
        
        // Shortcut out if the expunge has no effect on the vector
        if(size == 0 || indexVector.elementAt(size-1) < index) {
            return;
        }
        
        // Get the position where this index would be if it was in the vector.
        // This is the start point for our index shift.
        int p = indexVector.binarySearch(index, SimpleSortingIntVector.SORT_TYPE_NUMERIC);
        p = (p * -1) - 1; // Above returns: -(insertPoint + 1)
        
        // Iterate over the portion of the vector following this point,
        // and decrement the index values
        decrementIndexValues(p, updatedTokens);
    }

    private void decrementIndexValues(int startPosition, Vector updatedTokens) {
        int size = indexVector.size();
        
        for(int i=startPosition; i<size; i++) {
            // Get the old index value
            int oldIndex = indexVector.elementAt(i);
            int newIndex = oldIndex - 1;
            
            // Remove the mapped token, update it, and put it back in the map
            ImapMessageToken token = (ImapMessageToken)indexToTokenMap.remove(oldIndex);
            token.setMessageIndex(newIndex);
            indexToTokenMap.put(newIndex, token);
            updatedTokens.addElement(token);
            
            // Update the index in the vector
            indexVector.setElementAt(newIndex, i);
        }
    }
}
