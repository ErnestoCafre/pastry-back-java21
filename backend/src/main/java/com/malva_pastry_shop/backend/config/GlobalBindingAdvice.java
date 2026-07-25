package com.malva_pastry_shop.backend.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Normaliza el binding de formularios: recorta espacios y convierte los
 * strings vacios en null antes de la validacion.
 *
 * Evita que un campo opcional con restricciones de tamano falle al enviarse
 * vacio; por ejemplo, el password opcional en la edicion de usuario, que con
 * "" (cadena vacia) violaria @Size(min = 6) pero con null se omite.
 */
@ControllerAdvice
public class GlobalBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
