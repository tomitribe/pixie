/*
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
 */
package org.tomitribe.pixie.comp;

import org.tomitribe.util.PrintString;

import java.util.List;

public class MultipleComponentIssuesException extends ComponentException {

    private final List<? extends Throwable> issues;

    public MultipleComponentIssuesException(final Class component, final List<? extends Throwable> issues) {
        super(component);
        this.issues = issues;
    }

    @Override
    public String getMessage() {
        final PrintString message = new PrintString();
        message.printf("%s issues found%n", issues.size());

        int width = 0;
        for (final Throwable issue : issues) {
            width = Math.max(width, issue.getClass().getSimpleName().length());
        }

        for (final Throwable issue : issues) {
            message.printf(" - %-" + width + "s : %s%n", issue.getClass().getSimpleName(), issue.getMessage());
        }

        return message.toString();
    }

    public List<? extends Throwable> getIssues() {
        return issues;
    }
}
