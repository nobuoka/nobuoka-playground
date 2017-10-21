import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Positive;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Test {

    public static class T {
        @Positive(message = "${validatedValue} : ${value}")
        Integer test = -4;
        List<@Positive Integer> values = Arrays.asList(-1, 2, 3, -5);
    }

    public static void main(String[] args) {
        @Positive Integer test = -1;
        List<@Positive Integer> positiveIntegers = Arrays.asList(-1, 0, 2);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<T>> violations = validator.validate(new T());

        System.out.println("Violations : " + violations.size());
        violations.forEach(System.out::println);
        violations.forEach(v -> v.getPropertyPath().forEach(n -> System.out.println(n.getKind())));
    }

}

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE_USE, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@interface NotNull2 {
    String value() default "";
}
