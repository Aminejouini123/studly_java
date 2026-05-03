import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Paths;

public class FindModules {
    public static void main(String[] args) {
        ModuleFinder.of(Paths.get(args[0])).findAll().stream()
            .map(ModuleReference::descriptor)
            .forEach(d -> System.out.println(d.name()));
    }
}
