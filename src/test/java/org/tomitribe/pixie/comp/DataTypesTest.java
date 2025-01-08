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

import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DataTypesTest extends Assert {

    private final String config = "" +
            "joe = new://" + DataTypes.class.getName() + "\n" +
            "joe.aString = Hello\n" +
            "joe.abyte = 123\n" +
            "joe.ashort = 23456\n" +
            "joe.aint = 1234567890\n" +
            "joe.along = 1234567890123456789\n" +
            "joe.afloat = 1.234\n" +
            "joe.adouble = 0.1234567890\n" +
            "joe.aboolean = true\n" +
            "joe.achar = z\n" +
            "joe.aTimeUnit = SECONDS\n" +
            "joe.aURI = here://you.go\n" +
            "joe.aFile = DataTypesTest.txt\n";

    @Test
    public void test() throws IOException {
        final Properties properties = new Properties();
        properties.load(IO.read(config));

        final System system = new System(properties);

        final DataTypes actual = system.get(DataTypes.class);

        assertNotNull(actual);
        assertEquals("Hello", actual.getaString());
        assertEquals(123, actual.getAbyte());
        assertEquals(23456, actual.getAshort());
        assertEquals(1234567890, actual.getAint());
        assertEquals(1234567890123456789l, actual.getAlong());
        assertEquals(1.234f, actual.getAfloat(), 0);
        assertEquals(0.1234567890d, actual.getAdouble(), 0);
        assertEquals('z', actual.getAchar());
        assertEquals(TimeUnit.SECONDS, actual.getaTimeUnit());
        assertEquals(URI.create("here://you.go"), actual.getaURI());
    }


    public static class DataTypes {

        private final String aString;
        private final byte abyte;
        private final short ashort;
        private final int aint;
        private final long along;
        private final float afloat;
        private final double adouble;
        private final boolean aboolean;
        private final char achar;
        private final TimeUnit aTimeUnit;
        private final URI aURI;
        private final File aFile;

        public DataTypes(@Param("aString") String aString,
                         @Param("abyte") byte abyte,
                         @Param("ashort") short ashort,
                         @Param("aint") int aint,
                         @Param("along") long along,
                         @Param("afloat") float afloat,
                         @Param("adouble") double adouble,
                         @Param("aboolean") boolean aboolean,
                         @Param("achar") char achar,
                         @Param("aTimeUnit") TimeUnit aTimeUnit,
                         @Param("aURI") URI aURI,
                         @Param("aFile") File aFile
        ) {
            this.aString = aString;
            this.abyte = abyte;
            this.ashort = ashort;
            this.aint = aint;
            this.along = along;
            this.afloat = afloat;
            this.adouble = adouble;
            this.aboolean = aboolean;
            this.achar = achar;
            this.aTimeUnit = aTimeUnit;
            this.aURI = aURI;
            this.aFile = aFile;
        }

        public String getaString() {
            return aString;
        }

        public byte getAbyte() {
            return abyte;
        }

        public short getAshort() {
            return ashort;
        }

        public int getAint() {
            return aint;
        }

        public long getAlong() {
            return along;
        }

        public float getAfloat() {
            return afloat;
        }

        public double getAdouble() {
            return adouble;
        }

        public boolean isAboolean() {
            return aboolean;
        }

        public char getAchar() {
            return achar;
        }

        public TimeUnit getaTimeUnit() {
            return aTimeUnit;
        }

        public URI getaURI() {
            return aURI;
        }

        public File getaFile() {
            return aFile;
        }
    }
}