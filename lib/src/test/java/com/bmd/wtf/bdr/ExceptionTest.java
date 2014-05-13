/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.bdr;

import junit.framework.TestCase;

/**
 * Unit test for exception classes.
 * <p/>
 * Created by davide on 4/8/14.
 */
public class ExceptionTest extends TestCase {

    @SuppressWarnings("UnusedAssignment")
    public void testExceptions() {

        ClosedLoopException cex = new ClosedLoopException();
        cex = new ClosedLoopException("test");
        cex = new ClosedLoopException("test", cex);
        cex = new ClosedLoopException(cex);

        DelayInterruptedException dex = new DelayInterruptedException();
        dex = new DelayInterruptedException("test");
        dex = new DelayInterruptedException("test", dex);
        dex = new DelayInterruptedException(dex);

        DryStreamException yex = new DryStreamException();
        yex = new DryStreamException("test");
        yex = new DryStreamException("test", yex);
        yex = new DryStreamException(yex);

        DuplicateDamException uex = new DuplicateDamException();
        uex = new DuplicateDamException("test");
        uex = new DuplicateDamException("test", uex);
        uex = new DuplicateDamException(uex);

        FloatingException fex = new FloatingException();
        fex = new FloatingException("test");
        fex = new FloatingException("test", fex);
        fex = new FloatingException("test", fex, true, true);
        fex = new FloatingException(new Object());
        fex = new FloatingException(fex);

        UnauthorizedDischargeException aex = new UnauthorizedDischargeException();
        aex = new UnauthorizedDischargeException("test");
        aex = new UnauthorizedDischargeException("test", aex);
        aex = new UnauthorizedDischargeException(aex);
    }
}