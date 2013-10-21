/*
 * Copyright (c) 2013 David Boissier
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
 */

package org.codinjutsu.tools.mongo.view;

import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.util.ui.tree.TreeUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.codinjutsu.tools.mongo.model.MongoCollectionResult;
import org.codinjutsu.tools.mongo.view.nodedescriptor.MongoNodeDescriptor;
import org.fest.swing.driver.BasicJTableCellReader;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MongoResultPanelTest {

    private MongoResultPanel mongoResultPanel;

    private FrameFixture frameFixture;

    @After
    public void tearDown() {
        frameFixture.cleanUp();
    }

    @Before
    public void setUp() throws Exception {
        mongoResultPanel = GuiActionRunner.execute(new GuiQuery<MongoResultPanel>() {
            protected MongoResultPanel executeInEDT() {
                return new MongoResultPanel(DummyProject.getInstance(), new MongoRunnerPanel.MongoDocumentOperations() {
                    @Override
                    public void updateMongoDocument(DBObject mongoDocument) {

                    }

                    @Override
                    public DBObject getMongoDocument(ObjectId objectId) {
                        return new BasicDBObject();
                    }

                    @Override
                    public void deleteMongoDocument(DBObject mongoDocument) {

                    }
                });
            }
        });

        frameFixture = Containers.showInFrame(mongoResultPanel);
    }

    @Test
    public void displayTreeWithASimpleArray() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("simpleArray.json", "mycollec"));

        frameFixture.table("resultTreeTable").cellReader(new MyJTableCellReader())
                .requireColumnCount(2)
                .requireContents(new String[][]{
                        {"[0]", "\"toto\""},
                        {"[1]", "true"},
                        {"[2]", "10"},
                        {"[3]", "null"},
                });
    }

    @Test
    public void testDisplayTreeWithASimpleDocument() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("simpleDocument.json", "mycollec"));

        frameFixture.table("resultTreeTable").cellReader(new MyJTableCellReader())
                .requireColumnCount(2)
                .requireContents(new String[][]{
                        {"[0]", "{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"image\" :  null }"},
                        {"\"id\"", "0"},
                        {"\"label\"", "\"toto\""},
                        {"\"visible\"", "false"},
                        {"\"image\"", "null"}
                });
    }


    @Test
    public void testDisplayTreeWithAStructuredDocument() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("structuredDocument.json", "mycollec"));
        TreeUtil.expandAll(mongoResultPanel.resultTableView.getTree());
        frameFixture.table("resultTreeTable").cellReader(new MyJTableCellReader())
                .requireColumnCount(2)
                .requireContents(new String[][]{
                        {"[0]", "{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}}"},
                        {"\"id\"", "0"},
                        {"\"label\"", "\"toto\""},
                        {"\"visible\"", "false"},
                        {"\"doc\"", "{ \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}"},
                        {"\"title\"", "\"hello\""},
                        {"\"nbPages\"", "10"},
                        {"\"keyWord\"", "[ \"toto\" , true , 10]"},
                        {"[0]", "\"toto\""},
                        {"[1]", "true"},
                        {"[2]", "10"},
                });
    }


    @Test
    public void testDisplayTreeWithAnArrayOfStructuredDocument() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("arrayOfDocuments.json", "mycollec"));

        TreeUtil.expandAll(mongoResultPanel.resultTableView.getTree());
        frameFixture.table("resultTreeTable").cellReader(new MyJTableCellReader())
                .requireContents(new String[][]{

                        {"[0]", "{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}}"},
                        {"\"id\"", "0"},
                        {"\"label\"", "\"toto\""},
                        {"\"visible\"", "false"},
                        {"\"doc\"", "{ \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}"},
                        {"\"title\"", "\"hello\""},
                        {"\"nbPages\"", "10"},
                        {"\"keyWord\"", "[ \"toto\" , true , 10]"},
                        {"[0]", "\"toto\""},
                        {"[1]", "true"},
                        {"[2]", "10"},
                        {"[1]", "{ \"id\" : 1 , \"label\" : \"tata\" , \"visible\" : true , \"doc\" : { \"title\" : \"ola\" , \"nbPages\" : 1 , \"keyWord\" : [ \"tutu\" , false , 10]}}"},
                        {"\"id\"", "1"},
                        {"\"label\"", "\"tata\""},
                        {"\"visible\"", "true"},
                        {"\"doc\"", "{ \"title\" : \"ola\" , \"nbPages\" : 1 , \"keyWord\" : [ \"tutu\" , false , 10]}"},
                        {"\"title\"", "\"ola\""},
                        {"\"nbPages\"", "1"},
                        {"\"keyWord\"", "[ \"tutu\" , false , 10]"},
                        {"[0]", "\"tutu\""},
                        {"[1]", "false"},
                        {"[2]", "10"},
                });
    }

    @Test
    @Ignore
    public void testEditMongoDocument() throws Exception {
        MongoCollectionResult mongoCollectionResult = createCollectionResults("simpleDocumentForEdition.json", "mycollec");

//        Hack to convert an id into an ObjectId
//        DBObject document = mongoCollectionResult.getMongoObjects().get(0);
//        document.put("_id", new ObjectId(String.valueOf(document.get("_id"))));

        mongoResultPanel.updateResultTableTree(mongoCollectionResult);

        JTableFixture resultTableFixture =
                frameFixture.table("resultTreeTable")
                        .cellReader(new MyJTableCellReader())
                        .requireContents(new String[][]{
                                {"[0]", "{ \"_id\" : \"50b8d63414f85401b9268b99\" , \"label\" : \"toto\" , \"visible\" : false , \"image\" :  null }"},
                                {"\"_id\"", "\"50b8d63414f85401b9268b99\""},
                                {"\"label\"", "\"toto\""},
                                {"\"visible\"", "false"},
                                {"\"image\"", "null"}
                        });

        resultTableFixture.cell(resultTableFixture.cell("\"50b8d63414f85401b9268b99\"")).click();

        frameFixture.table("editionTreeTable").cellReader(new MyJTableCellReader())
                .requireColumnCount(2)
                .requireContents(new String[][]{
                        {"[0]", "{ \"_id\" : 50b8d63414f85401b9268b99 , \"label\" : \"toto\" , \"visible\" : false , \"image\" :  null }"},
                        {"\"_id\"", "50b8d63414f85401b9268b99"},
                        {"\"label\"", "\"toto\""},
                        {"\"visible\"", "false"},
                        {"\"image\"", "null"}
                });

    }

    @Test
    public void testCopyMongoObjectNodeValue() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("structuredDocument.json", "mycollec"));
        TreeUtil.expandAll(mongoResultPanel.resultTableView.getTree());

        mongoResultPanel.resultTableView.setRowSelectionInterval(0, 0);
        assertEquals("{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}}", mongoResultPanel.getSelectedNodeStringifiedValue());

        mongoResultPanel.resultTableView.setRowSelectionInterval(2, 2);
        assertEquals("{ \"label\" : \"toto\"}", mongoResultPanel.getSelectedNodeStringifiedValue());

        mongoResultPanel.resultTableView.setRowSelectionInterval(4, 4);
        assertEquals("{ \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}}", mongoResultPanel.getSelectedNodeStringifiedValue());
    }

    @Test
    public void copyMongoResults() throws Exception {
        mongoResultPanel.updateResultTableTree(createCollectionResults("arrayOfDocuments.json", "mycollec"));

        TreeUtil.expandAll(mongoResultPanel.resultTableView.getTree());

        frameFixture.table("resultTreeTable").cellReader(new MyJTableCellReader())
                .requireContents(new String[][]{
                        {"[0]", "{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}}"},
                        {"\"id\"", "0"},
                        {"\"label\"", "\"toto\""},
                        {"\"visible\"", "false"},
                        {"\"doc\"", "{ \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}"},
                        {"\"title\"", "\"hello\""},
                        {"\"nbPages\"", "10"},
                        {"\"keyWord\"", "[ \"toto\" , true , 10]"},
                        {"[0]", "\"toto\""},
                        {"[1]", "true"},
                        {"[2]", "10"},
                        {"[1]", "{ \"id\" : 1 , \"label\" : \"tata\" , \"visible\" : true , \"doc\" : { \"title\" : \"ola\" , \"nbPages\" : 1 , \"keyWord\" : [ \"tutu\" , false , 10]}}"},
                        {"\"id\"", "1"},
                        {"\"label\"", "\"tata\""},
                        {"\"visible\"", "true"},
                        {"\"doc\"", "{ \"title\" : \"ola\" , \"nbPages\" : 1 , \"keyWord\" : [ \"tutu\" , false , 10]}"},
                        {"\"title\"", "\"ola\""},
                        {"\"nbPages\"", "1"},
                        {"\"keyWord\"", "[ \"tutu\" , false , 10]"},
                        {"[0]", "\"tutu\""},
                        {"[1]", "false"},
                        {"[2]", "10"},
                });

        assertEquals("[ " +
                "{ \"id\" : 0 , \"label\" : \"toto\" , \"visible\" : false , \"doc\" : { \"title\" : \"hello\" , \"nbPages\" : 10 , \"keyWord\" : [ \"toto\" , true , 10]}} , " +
                "{ \"id\" : 1 , \"label\" : \"tata\" , \"visible\" : true , \"doc\" : { \"title\" : \"ola\" , \"nbPages\" : 1 , \"keyWord\" : [ \"tutu\" , false , 10]}}" +
                " ]",
                mongoResultPanel.getSelectedNodeStringifiedValue());
    }

    private MongoCollectionResult createCollectionResults(String data, String collectionName) throws IOException {
        DBObject jsonObject = (DBObject) JSON.parse(IOUtils.toString(getClass().getResourceAsStream(data)));

        MongoCollectionResult mongoCollectionResult = new MongoCollectionResult(collectionName);
        mongoCollectionResult.add(jsonObject);

        return mongoCollectionResult;
    }

    public class MyJTableCellReader extends BasicJTableCellReader {

        @Override
        public String valueAt(JTable table, int row, int column) {
            MongoNodeDescriptor nodeDescriptor = (MongoNodeDescriptor) table.getValueAt(row, column);
            if (column == 0) {
                return nodeDescriptor.getFormattedKey();
            }
            return nodeDescriptor.getFormattedValue();
        }
    }
}
