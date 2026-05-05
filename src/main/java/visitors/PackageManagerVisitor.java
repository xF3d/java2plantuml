package visitors;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.structure.PackageManager;

@AllArgsConstructor
public class PackageManagerVisitor extends VoidVisitorAdapter<Void> {
    private PackageManager packageManager;

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        addPackage(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        addPackage(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        addPackage(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(RecordDeclaration n, Void arg) {
        addPackage(n);
        super.visit(n, arg);
    }

    public void addPackage(TypeDeclaration<?> typeDeclaration) {
        packageManager.addClass(PackageManager.resolveTypeDeclaration(typeDeclaration));
    }
}
