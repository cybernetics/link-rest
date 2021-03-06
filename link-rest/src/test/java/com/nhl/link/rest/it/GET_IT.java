package com.nhl.link.rest.it;

import com.nhl.link.rest.DataResponse;
import com.nhl.link.rest.LinkRest;
import com.nhl.link.rest.it.fixture.JerseyTestOnDerby;
import com.nhl.link.rest.it.fixture.cayenne.E17;
import com.nhl.link.rest.it.fixture.cayenne.E19;
import com.nhl.link.rest.it.fixture.cayenne.E2;
import com.nhl.link.rest.it.fixture.cayenne.E3;
import com.nhl.link.rest.it.fixture.cayenne.E4;
import com.nhl.link.rest.it.fixture.cayenne.E6;
import com.nhl.link.rest.parser.converter.UtcDateConverter;
import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SQLTemplate;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GET_IT extends JerseyTestOnDerby {

    @Override
    protected void doAddResources(FeatureContext context) {
        context.register(Resource.class);
    }

    @Test
    public void testResponse() {

        insert("e4", "id, c_varchar, c_int", "1, 'xxx', 5");

        Response response = target("/e4").request().get();
        onSuccess(response).bodyEquals(1,
                "{\"id\":1,\"cBoolean\":null,\"cDate\":null,\"cDecimal\":null,"
                        + "\"cInt\":5,\"cTime\":null,\"cTimestamp\":null,\"cVarchar\":\"xxx\"}");
    }

    @Test
    public void testDateTime() {

        Date date = Date.from(Instant.from(UtcDateConverter.dateParser().fromString("2012-02-03T11:01:02Z")));

        SQLTemplate insert = new SQLTemplate(E4.class,
                "INSERT INTO utest.e4 (c_timestamp) values (#bind($ts 'TIMESTAMP'))");
        insert.setParams(Collections.singletonMap("ts", date));
        newContext().performGenericQuery(insert);

        Response response = target("/e4").queryParam("include", E4.C_TIMESTAMP.getName()).request().get();
        onSuccess(response).bodyEquals(1, "{\"cTimestamp\":\"2012-02-03T11:01:02Z\"}");
    }

    @Test
    public void testDate() {

        Date date = Date.from(Instant.from(UtcDateConverter.dateParser().fromString("2012-02-03")));

        SQLTemplate insert = new SQLTemplate(E4.class, "INSERT INTO utest.e4 (c_date) values (#bind($date 'DATE'))");
        insert.setParams(Collections.singletonMap("date", date));
        newContext().performGenericQuery(insert);

        Response response = target("/e4").queryParam("include", E4.C_DATE.getName()).request().get();
        onSuccess(response).bodyEquals(1, "{\"cDate\":\"2012-02-03\"}");
    }

    @Test
    public void testTime() {

        LocalTime lt = LocalTime.of(14, 0, 1);

        // "14:00:01"
        Time time = Time.valueOf(lt);

        SQLTemplate insert = new SQLTemplate(E4.class, "INSERT INTO utest.e4 (c_time) values (#bind($time 'TIME'))");
        insert.setParams(Collections.singletonMap("time", time));
        newContext().performGenericQuery(insert);

        Response response = target("/e4").queryParam("include", E4.C_TIME.getName()).request().get();
        onSuccess(response).bodyEquals(1, "{\"cTime\":\"14:00:01\"}");
    }

    @Test
    public void test_Sort_ById() {

        insert("e4", "id", "2");
        insert("e4", "id", "1");
        insert("e4", "id", "3");

        Response response = target("/e4")
                .queryParam("sort", urlEnc("[{\"property\":\"id\",\"direction\":\"DESC\"}]"))
                .queryParam("include", "id")
                .request()
                .get();

        onSuccess(response).bodyEquals(3, "{\"id\":3}", "{\"id\":2}", "{\"id\":1}");
    }

    @Test
    public void test_Sort_Invalid() {

        Response response = target("/e4")
                .queryParam("sort", urlEnc("[{\"property\":\"xyz\",\"direction\":\"DESC\"}]"))
                .queryParam("include", "id")
                .request()
                .get();

        onResponse(response)
                .statusEquals(Status.BAD_REQUEST)
                .bodyEquals("{\"success\":false,\"message\":\"Invalid path 'xyz' for 'E4'\"}");
    }

    @Test
    // this is a hack for Sencha bug, passing us null sorters per LF-189...
    // allowing for lax property name checking as a result
    public void test_Sort_Null() {

        insert("e4", "id", "2");
        insert("e4", "id", "1");
        insert("e4", "id", "3");

        Response response = target("/e4")
                .queryParam("sort", urlEnc("[{\"property\":null,\"direction\":\"DESC\"}]"))
                .queryParam("include", "id")
                .request()
                .get();

        onSuccess(response).totalEquals(3);
    }

    @Test
    public void test_SelectById() {

        insert("e4", "id", "2");

        Response response = target("/e4/2").request().get();

        onSuccess(response).bodyEquals(1, "{\"id\":2,\"cBoolean\":null," +
                "\"cDate\":null," +
                "\"cDecimal\":null," +
                "\"cInt\":null," +
                "\"cTime\":null," +
                "\"cTimestamp\":null," +
                "\"cVarchar\":null}");
    }

    @Test
    public void test_SelectById_Params() {

        insert("e4", "id", "2");

        Response response1 = target("/e4/2").request().get();
        onSuccess(response1).bodyEquals(1, "{\"id\":2,\"cBoolean\":null,\"cDate\":null,\"cDecimal\":null,"
                + "\"cInt\":null,\"cTime\":null,\"cTimestamp\":null,\"cVarchar\":null}");

        Response response2 = target("/e4/2").queryParam("include", "id").request().get();
        onSuccess(response2).bodyEquals(1, "{\"id\":2}");
    }

    @Test
    public void test_SelectById_NotFound() {

        Response response = target("/e4/2").request().get();
        onResponse(response).statusEquals(Status.NOT_FOUND)
                .bodyEquals("{\"success\":false,\"message\":\"No object for ID '2' and entity 'E4'\"}");
    }

    @Test
    public void test_SelectById_Prefetching() {

        insert("e2", "id, name", "1, 'xxx'");
        insert("e3", "id, name, e2_id", "8, 'yyy', 1");
        insert("e3", "id, name, e2_id", "9, 'zzz', 1");

        Response response1 = target("/e3/8").queryParam("include", "e2.id").request().get();
        onSuccess(response1).bodyEquals(1, "{\"id\":8,\"e2\":{\"id\":1},\"name\":\"yyy\",\"phoneNumber\":null}");

        Response response2 = target("/e3/8").queryParam("include", "e2.name").request().get();
        onSuccess(response2).bodyEquals(1, "{\"id\":8,\"e2\":{\"name\":\"xxx\"},\"name\":\"yyy\",\"phoneNumber\":null}");

        Response response3 = target("/e2/1").queryParam("include", "e3s.id").request().get();
        onSuccess(response3).bodyEquals(1, "{\"id\":1,\"address\":null,\"e3s\":[{\"id\":8},{\"id\":9}],\"name\":\"xxx\"}");
    }

    @Test
    public void test_Select_Prefetching() {

        insert("e2", "id, name", "1, 'xxx'");
        insert("e3", "id, name, e2_id", "8, 'yyy', 1");
        insert("e3", "id, name, e2_id", "9, 'zzz', 1");

        Response response = target("/e3")
                .queryParam("include", "id")
                .queryParam("include", "e2.id")
                .queryParam("sort", "id")
                .request()
                .get();

        onSuccess(response).bodyEquals(2, "{\"id\":8,\"e2\":{\"id\":1}}", "{\"id\":9,\"e2\":{\"id\":1}}");
    }

    @Test
    public void test_Select_RelationshipSort() {

        insert("e2", "id, name", "1, 'zzz'");
        insert("e2", "id, name", "2, 'yyy'");
        insert("e2", "id, name", "3, 'xxx'");

        insert("e3", "id, name, e2_id", "8, 'aaa', 1");
        insert("e3", "id, name, e2_id", "9, 'bbb', 2");
        insert("e3", "id, name, e2_id", "10, 'ccc', 3");

        Response response = target("/e3")
                .queryParam("include", "id")
                .queryParam("include", E3.E2.getName())
                .queryParam("sort", E3.E2.dot(E2.NAME).getName())
                .request()
                .get();

        onSuccess(response).bodyEquals(3,
                "{\"id\":10,\"e2\":{\"id\":3,\"address\":null,\"name\":\"xxx\"}}",
                "{\"id\":9,\"e2\":{\"id\":2,\"address\":null,\"name\":\"yyy\"}}",
                "{\"id\":8,\"e2\":{\"id\":1,\"address\":null,\"name\":\"zzz\"}}");
    }

    @Test
    public void test_Select_RelationshipStartLimit() throws UnsupportedEncodingException {

        insert("e2", "id, name", "1, 'zzz'");
        insert("e2", "id, name", "2, 'yyy'");

        insert("e3", "id, name, e2_id", "8, 'aaa', 1");
        insert("e3", "id, name, e2_id", "9, 'bbb', 1");
        insert("e3", "id, name, e2_id", "10, 'ccc', 2");

        Response response = target("/e2")
                .queryParam("include", "id")
                .queryParam("include", URLEncoder.encode("{\"path\":\"" + E2.E3S.getName() + "\",\"start\":1,\"limit\":1}", "UTF-8"))
                .queryParam("exclude", E2.E3S.dot(E3.PHONE_NUMBER).getName())
                .request().get();

        onSuccess(response).bodyEquals(2,
                "{\"id\":1,\"e3s\":[{\"id\":9,\"name\":\"bbb\"}]}",
                "{\"id\":2,\"e3s\":[]}");
    }

    @Test
    public void test_Select_Prefetching_StartLimit() {

        insert("e2", "id, name", "1, 'xxx'");

        insert("e3", "id, name, e2_id", "8, 'yyy', 1");
        insert("e3", "id, name, e2_id", "9, 'zzz', 1");
        insert("e3", "id, name, e2_id", "10, 'zzz', 1");
        insert("e3", "id, name, e2_id", "11, 'zzz', 1");

        Response response = target("/e3")
                .queryParam("include", "id", "e2.id")
                .queryParam("sort", "id")
                .queryParam("start", "1")
                .queryParam("limit", "2")
                .request()
                .get();

        onSuccess(response).bodyEquals(4,
                "{\"id\":9,\"e2\":{\"id\":1}}",
                "{\"id\":10,\"e2\":{\"id\":1}}");
    }

    @Test
    public void test_SelectToOne_Null() {

        insert("e2", "id, name", "1, 'xxx'");

        insert("e3", "id, name, e2_id", "8, 'yyy', 1");
        insert("e3", "id, name, e2_id", "9, 'zzz', null");

        Response response = target("/e3")
                .queryParam("include", "e2.id", "id")
                .request()
                .get();

        onSuccess(response).bodyEquals(2,
                "{\"id\":8,\"e2\":{\"id\":1}}",
                "{\"id\":9,\"e2\":null}");
    }

    @Test
    public void test_SelectCharPK() {

        insert("e6", "char_id, char_column", "'a', 'aaa'");

        Response response = target("/e6/a").request().get();
        onSuccess(response).bodyEquals(1, "{\"id\":\"a\",\"charColumn\":\"aaa\"}");
    }

    @Test
    public void test_SelectByCompoundId() {

        insert("e17", "id1, id2, name", "1, 1, 'aaa'");

        Response response = target("/e17")
                .queryParam("id1", 1)
                .queryParam("id2", 1)
                .request()
                .get();

        onSuccess(response).bodyEquals(1, "{\"id\":{\"id1\":1,\"id2\":1},\"id1\":1,\"id2\":1,\"name\":\"aaa\"}");
    }

    @Test
    public void test_Select_MapByRootEntity() {

        insert("e4", "c_varchar, c_int", "'xxx', 1");
        insert("e4", "c_varchar, c_int", "'yyy', 2");
        insert("e4", "c_varchar, c_int", "'zzz', 2");

        Response response = target("/e4")
                .queryParam("mapBy", E4.C_INT.getName())
                .queryParam("include", E4.C_VARCHAR.getName())
                .request()
                .get();

        onSuccess(response).bodyEqualsMapBy(3,
                "\"1\":[{\"cVarchar\":\"xxx\"}]",
                "\"2\":[{\"cVarchar\":\"yyy\"},{\"cVarchar\":\"zzz\"}]");
    }

    @Test
    public void test_Select_MapByRootEntity_Related() {

        insert("e2", "id, name", "1, 'zzz'");
        insert("e2", "id, name", "2, 'yyy'");
        insert("e3", "id, e2_id, name", "8,  1, 'aaa'");
        insert("e3", "id, e2_id, name", "9,  1, 'bbb'");
        insert("e3", "id, e2_id, name", "10, 2, 'ccc'");

        Response response = target("/e3")
                .queryParam("mapBy", E3.E2.dot(E2.ID_PK_COLUMN).getName())
                .queryParam("exclude", E3.PHONE_NUMBER.getName())
                .request()
                .get();

        onSuccess(response).bodyEqualsMapBy(3,
                "\"1\":[{\"id\":8,\"name\":\"aaa\"},{\"id\":9,\"name\":\"bbb\"}]",
                "\"2\":[{\"id\":10,\"name\":\"ccc\"}]");
    }

    @Test
    public void test_SelectById_EscapeLineSeparators() {

        insert("e4", "id, c_varchar", "1, 'First line\u2028Second line...\u2029'");

        Response response = target("/e4/1")
                .queryParam("include", "cVarchar")
                .request()
                .get();

        onSuccess(response).bodyEquals(1, "{\"cVarchar\":\"First line\\u2028Second line...\\u2029\"}");
    }

    @Test
    public void test_SelectByteArrayProperty() throws IOException {

        ObjectContext ctx = newContext();
        E19 e19 = ctx.newObject(E19.class);
        e19.setGuid("someValue123".getBytes("UTF-8"));
        ctx.commitChanges();

        Response response = target("/e19/" + Cayenne.intPKForObject(e19))
                .queryParam("include", E19.GUID.getName())
                .request()
                .get();

        onSuccess(response).bodyEquals(1, "{\"guid\":\"c29tZVZhbHVlMTIz\"}");
    }

    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public static class Resource {

        @Context
        private Configuration config;

        @GET
        @Path("e2")
        public DataResponse<E2> getE2(@Context UriInfo uriInfo) {
            return LinkRest.service(config).select(E2.class).uri(uriInfo).get();
        }

        @GET
        @Path("e2/{id}")
        public DataResponse<E2> getE2ById(@PathParam("id") int id, @Context UriInfo uriInfo) {
            return LinkRest.service(config).selectById(E2.class, id, uriInfo);
        }

        @GET
        @Path("e3")
        public DataResponse<E3> getE3(@Context UriInfo uriInfo) {
            return LinkRest.service(config).select(E3.class).uri(uriInfo).get();
        }

        @GET
        @Path("e3/{id}")
        public DataResponse<E3> getE3ById(@PathParam("id") int id, @Context UriInfo uriInfo) {
            return LinkRest.service(config).selectById(E3.class, id, uriInfo);
        }

        @GET
        @Path("e4")
        public DataResponse<E4> getE4(@Context UriInfo uriInfo) {
            return LinkRest.service(config).select(E4.class).uri(uriInfo).get();
        }

        @GET
        @Path("e4/{id}")
        public DataResponse<E4> getE4_WithIncludeExclude(@PathParam("id") int id, @Context UriInfo uriInfo) {
            return LinkRest.service(config).selectById(E4.class, id, uriInfo);
        }

        @GET
        @Path("e6/{id}")
        public DataResponse<E6> getOneE6(@PathParam("id") String id) {
            return LinkRest.service(config).selectById(E6.class, id);
        }

        @GET
        @Path("e19/{id}")
        public DataResponse<E19> getById(@Context UriInfo uriInfo, @PathParam("id") Integer id) {
            return LinkRest.select(E19.class, config).uri(uriInfo).byId(id).getOne();
        }

        @GET
        @Path("e17")
        public DataResponse<E17> getByCompoundId(
                @Context UriInfo uriInfo,
                @QueryParam("id1") Integer id1,
                @QueryParam("id2") Integer id2) {

            Map<String, Object> ids = new HashMap<>();
            ids.put(E17.ID1_PK_COLUMN, id1);
            ids.put(E17.ID2_PK_COLUMN, id2);

            return LinkRest.select(E17.class, config).uri(uriInfo).byId(ids).getOne();
        }
    }

}
