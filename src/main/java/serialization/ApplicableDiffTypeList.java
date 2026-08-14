package serialization;

import ai.reponse.ApplicableDiff;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class ApplicableDiffTypeList extends TypeReference<List<ApplicableDiff>> {
    public static final ApplicableDiffTypeList TYPE = new ApplicableDiffTypeList();

    private ApplicableDiffTypeList() {}
}
