package translate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import translate.component.formatter.MemberFormatter;
import translate.structure.uml.PackageFilter;

@Accessors(chain = true)
@Setter
@Getter
@NoArgsConstructor
public class TranslatorsConfig {

    // uml stuff
    private boolean showMethods;
    private boolean showAttributes;
    private boolean showColoredAccessSpecifiers;
    private boolean showAssociations;

    private final PackageFilter packageFilter = new PackageFilter();

    private boolean skipTests;

    public TranslatorsConfig withBlacklist(String entry) {
        packageFilter.add2Bl(entry.replace(".", MemberFormatter.PACKAGE_DELIMITER));
        return this;
    }

    public TranslatorsConfig withWhitelist(String entry) {
        packageFilter.add2Wl(entry.replace(".", MemberFormatter.PACKAGE_DELIMITER));
        return this;
    }
}
