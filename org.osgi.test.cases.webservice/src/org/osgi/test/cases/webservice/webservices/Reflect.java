/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.webservice.webservices;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public class Reflect {
    @WebMethod
    public String reflect(String message) {
        int[] array = message.codePoints().toArray();
        
        int midpoint = array.length / 2;
        
        for(int i = 0; i < midpoint; i++) {
        	int tmp = array[i];
        	int swapIdx = array.length - i - 1;
			array[i] = array[swapIdx];
			array[swapIdx] = tmp;
        }
        return new String(array, 0, array.length);
    }
}