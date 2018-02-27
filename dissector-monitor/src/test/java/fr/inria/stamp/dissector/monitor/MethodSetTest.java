package fr.inria.stamp.dissector.monitor;

public class MethodSetTest {

//    @Test
//    public void testFromMutationFile() throws IOException {
//
//        String[] expectedMethods = {
//                "path.to.class.TheClass.partiallyTestedMethod(java.lang.String)",
//                "path.to.class.TheClass.pseudoTestedMethod()",
//                "path.to.class.TheClassTest.test1()",
//                "path.to.class.TheClassTest.test2()",
//                "path.to.class.TheClassTest.test4()"
//        };
//
//        File mutationFile = new File(getClass().getClassLoader().getResource("mutations.json").getFile());
//
//        MethodSet set = MethodSet.fromMutationFile(mutationFile);
//        List<String> methods = set.getMethods();
//
//        assertThat(methods, hasItems(expectedMethods));
//        assertThat(methods, hasSize(expectedMethods.length)); // Contains no other method
//
//        for (String method : expectedMethods) {
//            boolean shouldBeTest = method.contains("TheClassTest.test");
//            boolean isMarkedAsTest = set.isTest(method);
//
//            assertTrue(method + " misclassified",
//                    ((shouldBeTest && isMarkedAsTest) || (!shouldBeTest && !isMarkedAsTest)));
//        }
//
//    }

}