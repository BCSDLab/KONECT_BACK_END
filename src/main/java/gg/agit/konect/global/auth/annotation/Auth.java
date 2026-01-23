package gg.agit.konect.global.auth.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import gg.agit.konect.domain.user.enums.UserRole;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Auth {

    UserRole[] roles();
}
