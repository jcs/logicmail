/*-
 * Copyright (c) 2010, Derek Konigsberg
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
package org.logicprobe.LogicMail.mail;

public class FolderExpungedEvent extends FolderEvent {
    private final int[] indices;
    private final MessageToken[] expungedTokens;
    private final MessageToken[] updatedTokens;
    
    public FolderExpungedEvent(Object source, FolderTreeItem folder, int[] indices, MessageToken[] updatedTokens) {
        super(source, folder);
        this.indices = indices;
        this.expungedTokens = null;
        this.updatedTokens = updatedTokens;
    }
    
    public FolderExpungedEvent(Object source, FolderTreeItem folder, MessageToken[] expungedTokens, MessageToken[] updatedTokens) {
        super(source, folder);
        this.indices = null;
        this.expungedTokens = expungedTokens;
        this.updatedTokens = updatedTokens;
    }

    /**
     * Gets the indices of the expunged messages.
     *
     * @return the expunged indices, or null if none available
     */
    public int[] getExpungedIndices() {
        return indices;
    }
    
    /**
     * Gets the tokens of the expunged messages.
     *
     * @return the expunged tokens, or null if none available
     */
    public MessageToken[] getExpungedTokens() {
        return expungedTokens;
    }
    
    /**
     * Gets the non-expunged tokens updated as a result of the expunge operation.
     *
     * @return the updated tokens, or an empty array if none available
     */
    public MessageToken[] getUpdatedTokens() {
        return updatedTokens;
    }
}
