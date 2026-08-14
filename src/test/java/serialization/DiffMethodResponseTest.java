package serialization;

import ai.reponse.ApplicableDiff;
import ai.reponse.DiffMethodResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DiffMethodResponseTest {

    @Test
    public void serialize() {
        DiffMethodResponse response = new DiffMethodResponse("testing", "whatever(int)", List.of(
                "public void test() {",
                "}"
        ), List.of("Suggestion: Test"));

        JacksonMapper.init();
        try {
            JacksonMapper.getMapper().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deserialize() {
        DiffMethodResponse response = new DiffMethodResponse("testing", "whatever(int)", List.of(
                "public void test() {",
                "}"
        ), List.of("Suggestion: Test"));

        JacksonMapper.init();
        String serialized;
        try {
            serialized = JacksonMapper.getMapper().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        try {
            Assert.assertEquals(response, JacksonMapper.getMapper().readValue(serialized, ApplicableDiff.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void serializeList() {
        DiffMethodResponse response1 = new DiffMethodResponse("testing", "whatever(int)", List.of(
                "public void test() {",
                "}"
        ), List.of("Suggestion: Test"));

        DiffMethodResponse response2 = new DiffMethodResponse("another::testing", "whatever(int)", List.of(
                "public void test() {",
                "}"
        ), List.of("Suggestion: Test"));

        List<DiffMethodResponse> responses = List.of(response1, response2);
        JacksonMapper.init();


        try {
            JacksonMapper.getMapper()
                    .writerFor(ApplicableDiffTypeList.TYPE)
                    .writeValueAsString(responses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
