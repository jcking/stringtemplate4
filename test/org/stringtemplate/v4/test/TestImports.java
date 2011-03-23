/*
 [The "BSD license"]
 Copyright (c) 2009 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.stringtemplate.v4.test;

import org.junit.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.STGroupFile;

import static org.junit.Assert.assertEquals;

public class TestImports extends BaseTest {
	@Test public void testImportDir() throws Exception {
		/*
		dir1
			g.stg has a() that imports dir2 with absolute path
		dir2
			a.st
			b.st
		 */
		String dir1 = getRandomDir()+"/dir1";
		String dir2 = getRandomDir()+"/dir2";
		String gstr =
			"import \""+dir2+"\"\n" +
			"a() ::= <<dir1 a>>\n";
		writeFile(dir1, "g.stg", gstr);

		String a = "a() ::= <<dir2 a>>\n";
		String b = "b() ::= <<dir2 b>>\n";
		writeFile(dir2, "a.st", a);
		writeFile(dir2, "b.st", b);

		STGroup group = new STGroupFile(dir1+"/g.stg");
		ST st = group.getInstanceOf("b"); // visible only if import worked
		String expected = "dir2 b";
		String result = st.render();
		assertEquals(expected, result);
	}

	@Test public void testImportDirInJarViaCLASSPATH() throws Exception {
		/*
		sub
			g.stg has a() that imports dir2 via classpath
		base
			a.st
			b.st
		 */
		String root = getRandomDir();
		String sub = root +"/sub";
		String base = root +"/base";
		String gstr =
			"import \"base\"\n" +
			"a() ::= <<sub a>>\n";
		writeFile(sub, "g.stg", gstr);

		String a = "a() ::= <<base a>>\n";
		String b = "b() ::= <<base b>>\n";
		writeFile(base, "a.st", a);
		writeFile(base, "b.st", b);

		writeTestFile(
			"STGroup group = new STGroupFile(\"sub/g.stg\");\n" +
			"ST st = group.getInstanceOf(\"b\");\n"+ // visible only if import worked
			"String result = st.render();\n",
			root);
		compile("Test.java", root);
		// jar has sub, base as dirs in CLASSPATH
		jar("test.jar", new String[]{"sub", "base"}, root);
		Runtime.getRuntime().exec("rm -rf "+root+"/sub "+root+"/base");
		String result = java("Test", "test.jar", root);

		String expected = "base b"+newline;
		assertEquals(expected, result);
	}

	@Test public void testImportGroupAtSameLevelInJar() throws Exception {
		/*
		org/foo/templates
			main.stg imports lib.stg
			lib.stg
		 */
		String root = getRandomDir();
		System.out.println(root);
		String dir = root+"/org/foo/templates";
		String main =
			"import \"lib.stg\"\n" + // should see in same dir as main.stg
			"a() ::= <<main a calls <bold()>!>>\n"+
			"b() ::= <<main b>>\n";
		writeFile(dir, "main.stg", main);

		String lib =
			"bold() ::= <<lib bold>>\n";
		writeFile(dir, "lib.stg", lib);

		writeTestFile(
			"STGroup group = new STGroupFile(\"org/foo/templates/main.stg\");\n" +
			"ST st = group.getInstanceOf(\"a\");\n"+
			"String result = st.render();\n",
			root);
		compile("Test.java", root);
		jar("test.jar", new String[] {"org"}, root);
		Runtime.getRuntime().exec("rm -rf "+root+"/org");
		String result = java("Test", "test.jar", root);

		String expected = "main a calls lib bold!"+newline;
		assertEquals(expected, result);
	}

	@Test public void testImportGroupInJarViaCLASSPATH() throws Exception {
		/*
		org/foo/templates
			main.stg imports org/foo/lib/lib.stg
		org/foo/lib
			lib.stg
		 */
		String root = getRandomDir();
		System.out.println(root);
		String dir = root+"/org/foo/templates";
		String main =
			"import \"org/foo/lib/lib.stg\"\n" +
			"a() ::= <<main a calls <bold()>!>>\n"+
			"b() ::= <<main b>>\n";
		writeFile(dir, "main.stg", main);

		String lib =
			"bold() ::= <<lib bold>>\n";
		dir = root+"/org/foo/lib";
		writeFile(dir, "lib.stg", lib);

		writeTestFile(
			"STGroup group = new STGroupFile(\"org/foo/templates/main.stg\");\n" +
			"ST st = group.getInstanceOf(\"a\");\n"+
			"String result = st.render();\n",
			root);
		compile("Test.java", root);
		jar("test.jar", new String[] {"org"}, root);
		Runtime.getRuntime().exec("rm -rf "+root+"/org");
		String result = java("Test", "test.jar", root);

		String expected = "main a calls lib bold!"+newline;
		assertEquals(expected, result);
	}

	@Test public void testImportTemplateFileInJarViaCLASSPATH() throws Exception {
		/*
		org/foo/templates
			main.stg imports foo.st
		foo.st
		 */
		String root = getRandomDir();
		System.out.println(root);
		String dir = root+"/org/foo/templates";
		String main =
			"import \"foo.st\"\n" + // should see in same dir as main.stg
			"a() ::= <<main a calls <foo()>!>>\n"+
			"b() ::= <<main b>>\n";
		writeFile(dir, "main.stg", main);

		String foo =
			"foo() ::= <<foo>>\n";
		writeFile(root, "foo.st", foo);

		writeTestFile(
			"STGroup group = new STGroupFile(\"org/foo/templates/main.stg\");\n" +
			"ST st = group.getInstanceOf(\"a\");\n"+
			"String result = st.render();\n",
			root);
		compile("Test.java", root);
		jar("test.jar", new String[] {"org"}, root);
		Runtime.getRuntime().exec("rm -rf "+root+"/org");
		String result = java("Test", "test.jar", root);

		String expected = "main a calls foo!"+newline;
		assertEquals(expected, result);
	}

	@Test public void testImportRelativeDir() throws Exception {
		/*
		dir
			g.stg has a() that imports subdir with relative path
			subdir
				a.st
				b.st
		 */
		String dir = getRandomDir();
		String gstr =
			"import \"subdir\"\n" + // finds subdir in dir
			"a() ::= <<dir1 a>>\n";
		writeFile(dir, "g.stg", gstr);

		String a = "a() ::= <<dir2 a>>\n";
		String b = "b() ::= <<dir2 b>>\n";
		writeFile(dir, "subdir/a.st", a);
		writeFile(dir, "subdir/b.st", b);

		STGroup group = new STGroupFile(dir +"/g.stg");
		ST st = group.getInstanceOf("b"); // visible only if import worked
		String expected = "dir2 b";
		String result = st.render();
		assertEquals(expected, result);
	}

	@Test public void testImportGroupFileSameDir() throws Exception {
		/*
		dir
			group1.stg		that imports group2.stg in same dir with just filename
			group2.stg		has c()
		 */
		String dir = getRandomDir();
		String groupFile =
			"import \"group2.stg\"\n" +
			"a() ::= \"g1 a\"\n"+
			"b() ::= \"<c()>\"\n";
		writeFile(dir, "group1.stg", groupFile);

		groupFile =
			"c() ::= \"g2 c\"\n";
		writeFile(dir, "group2.stg", groupFile);

		STGroup group1 = new STGroupFile(dir+"/group1.stg");
		ST st = group1.getInstanceOf("c"); // should see c()
		String expected = "g2 c";
		String result = st.render();
		assertEquals(expected, result);
	}

	@Test public void testImportRelativeGroupFile() throws Exception {
		/*
		dir
			group1.stg		that imports group2.stg in same dir with just filename
			subdir
				group2.stg	has c()
		 */
		String dir = getRandomDir();
		String groupFile =
			"import \"subdir/group2.stg\"\n" +
			"a() ::= \"g1 a\"\n"+
			"b() ::= \"<c()>\"\n";
		writeFile(dir, "group1.stg", groupFile);

		groupFile =
			"c() ::= \"g2 c\"\n";
		writeFile(dir, "subdir/group2.stg", groupFile);

		STGroup group1 = new STGroupFile(dir+"/group1.stg");
		ST st = group1.getInstanceOf("c"); // should see c()
		String expected = "g2 c";
		String result = st.render();
		assertEquals(expected, result);
	}

	@Test public void testImportTemplateFileSameDir() throws Exception {
		/*
		dir
			group1.stg		that imports c.st
			c.st
		 */
		String dir = getRandomDir();
		String groupFile =
			"import \"c.st\"\n" +
			"a() ::= \"g1 a\"\n"+
			"b() ::= \"<c()>\"\n";
		writeFile(dir, "group1.stg", groupFile);

		groupFile =
			"c() ::= \"c\"\n";
		writeFile(dir, "c.st", groupFile);

		STGroup group1 = new STGroupFile(dir+"/group1.stg");
		ST st = group1.getInstanceOf("c"); // should see c()
		String expected = "c";
		String result = st.render();
		assertEquals(expected, result);
	}

	@Test public void testImportRelativeTemplateFile() throws Exception {
		/*
		dir
			group1.stg		that imports c.st
			subdir
				c.st
		 */
		String dir = getRandomDir();
		String groupFile =
			"import \"subdir/c.st\"\n" +
			"a() ::= \"g1 a\"\n"+
			"b() ::= \"<c()>\"\n";
		writeFile(dir, "group1.stg", groupFile);

		String stFile =
			"c() ::= \"c\"\n";
		writeFile(dir, "subdir/c.st", stFile);

		STGroup group1 = new STGroupFile(dir+"/group1.stg");
		ST st = group1.getInstanceOf("c"); // should see c()
		String expected = "c";
		String result = st.render();
		assertEquals(expected, result);
	}



	@Test public void testImportTemplateFromAnotherGroupObject() throws Exception {
		/*
		dir1
			a.st
			b.st
		dir2
			a.st
		 */
		String dir1 = getRandomDir();
		String a = "a() ::= <<dir1 a>>\n";
		String b = "b() ::= <<dir1 b>>\n";
		writeFile(dir1, "a.st", a);
		writeFile(dir1, "b.st", b);
		String dir2 = getRandomDir();
		a = "a() ::= << <b()> >>\n";
		writeFile(dir2, "a.st", a);

		STGroup group1 = new STGroupDir(dir1);
		STGroup group2 = new STGroupDir(dir2);
		group2.importTemplates(group1);
		ST st = group2.getInstanceOf("b");
		String expected = "dir1 b";
		String result = st.render();
		assertEquals(expected, result);

		// do it again, but make a template ref imported template
		st = group2.getInstanceOf("a");
		expected = " dir1 b ";
		result = st.render();
		assertEquals(expected, result);
	}

    @Test public void testImportTemplateInGroupFileFromDir() throws Exception {
		/*
		dir
			x
				a.st
			y
				group.stg		has b, c
		 */
        String dir = getRandomDir();
        String a = "a() ::= << <b()> >>\n";
        writeFile(dir, "x/a.st", a);

        String groupFile =
            "b() ::= \"group file b\"\n"+
            "c() ::= \"group file c\"\n";
        writeFile(dir, "y/group.stg", groupFile);

        STGroup group1 = new STGroupDir(dir+"/x");
        STGroup group2 = new STGroupFile(dir+"/y/group.stg");
        group1.importTemplates(group2);
        ST st = group1.getInstanceOf("a");
        String expected = " group file b ";
        String result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testImportTemplateInGroupFileFromGroupFile() throws Exception {
        String dir = getRandomDir();
        String groupFile =
            "a() ::= \"g1 a\"\n"+
            "b() ::= \"<c()>\"\n";
        writeFile(dir, "x/group.stg", groupFile);

        groupFile =
            "b() ::= \"g2 b\"\n"+
            "c() ::= \"g2 c\"\n";
        writeFile(dir, "y/group.stg", groupFile);

        STGroup group1 = new STGroupFile(dir+"/x/group.stg");
        STGroup group2 = new STGroupFile(dir+"/y/group.stg");
        group1.importTemplates(group2);
        ST st = group1.getInstanceOf("b");
        String expected = "g2 c";
        String result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testImportTemplateFromSubdir() throws Exception {
        // /randomdir/x/subdir/a and /randomdir/y/subdir/b
        String dir = getRandomDir();
        String a = "a() ::= << <subdir/b()> >>\n";
        String b = "b() ::= <<x's subdir/b>>\n";
        writeFile(dir, "x/subdir/a.st", a);
        writeFile(dir, "y/subdir/b.st", b);

        STGroup group1 = new STGroupDir(dir+"/x");
        STGroup group2 = new STGroupDir(dir+"/y");
        group1.importTemplates(group2);
        ST st = group1.getInstanceOf("subdir/a");
        String expected = " x's subdir/b ";
        String result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testImportTemplateFromGroupFile() throws Exception {
        // /randomdir/x/subdir/a and /randomdir/y/subdir.stg which has a and b
        String dir = getRandomDir();
        String a = "a() ::= << <subdir/b()> >>\n"; // get b imported from subdir.stg
        writeFile(dir, "x/subdir/a.st", a);

        String groupFile =
            "a() ::= \"group file: a\"\n"+
            "b() ::= \"group file: b\"\n";
        writeFile(dir, "y/subdir.stg", groupFile);

        STGroup group1 = new STGroupDir(dir+"/x");
        STGroup group2 = new STGroupDir(dir+"/y");
        group1.importTemplates(group2);
        ST st = group1.getInstanceOf("subdir/a");
        String expected = " group file: b ";
        String result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testPolymorphicTemplateReference() throws Exception {
        String dir1 = getRandomDir();
        String b = "b() ::= <<dir1 b>>\n";
        writeFile(dir1, "b.st", b);
        String dir2 = getRandomDir();
        String a = "a() ::= << <b()> >>\n";
        b = "b() ::= <<dir2 b>>\n";
        writeFile(dir2, "a.st", a);
        writeFile(dir2, "b.st", b);

        STGroup group1 = new STGroupDir(dir1);
        STGroup group2 = new STGroupDir(dir2);
        group1.importTemplates(group2);

        // normal lookup; a created from dir2 calls dir2.b
        ST st = group2.getInstanceOf("a");
        String expected = " dir2 b ";
        String result = st.render();
        assertEquals(expected, result);

        // polymorphic lookup; a created from dir1 calls dir2.a which calls dir1.b
        st = group1.getInstanceOf("a");
        expected = " dir1 b ";
        result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testSuper() throws Exception {
        String dir1 = getRandomDir();
        String a = "a() ::= <<dir1 a>>\n";
        String b = "b() ::= <<dir1 b>>\n";
        writeFile(dir1, "a.st", a);
        writeFile(dir1, "b.st", b);
        String dir2 = getRandomDir();
        a = "a() ::= << [<super.a()>] >>\n";
        writeFile(dir2, "a.st", a);

        STGroup group1 = new STGroupDir(dir1);
        STGroup group2 = new STGroupDir(dir2);
        group2.importTemplates(group1);
        ST st = group2.getInstanceOf("a");
        String expected = " [dir1 a] ";
        String result = st.render();
        assertEquals(expected, result);
    }

	@Test public void testUnloadImportedTemplate() throws Exception {
		String dir1 = getRandomDir();
		String a = "a() ::= <<dir1 a>>\n";
		String b = "b() ::= <<dir1 b>>\n";
		writeFile(dir1, "a.st", a);
		writeFile(dir1, "b.st", b);
		String dir2 = getRandomDir();
		a = "a() ::= << <b()> >>\n";
		writeFile(dir2, "a.st", a);

		STGroup group1 = new STGroupDir(dir1);
		STGroup group2 = new STGroupDir(dir2);
		group2.importTemplates(group1);

		ST st = group2.getInstanceOf("a");
		ST st2 = group2.getInstanceOf("b");
		int originalHashCode = System.identityHashCode(st);
		int originalHashCode2 = System.identityHashCode(st2);
		group1.unload(); // blast cache
		st = group2.getInstanceOf("a");
		int newHashCode = System.identityHashCode(st);
		assertEquals(originalHashCode==newHashCode, false); // diff objects

		String expected = " dir1 b ";
		String result = st.render();
		assertEquals(expected, result);

		st = group2.getInstanceOf("b");
		int newHashCode2 = System.identityHashCode(st);
		assertEquals(originalHashCode2==newHashCode2, false); // diff objects
		result = st.render();
		expected = "dir1 b";
		assertEquals(expected, result);
	}
}
