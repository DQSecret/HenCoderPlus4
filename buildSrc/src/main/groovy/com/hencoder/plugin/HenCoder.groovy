package com.hencoder.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class HenCoder implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def ext = project.extensions.create("hencoder", HenCoderExtension)
        project.afterEvaluate {
            println("Hello ${ext.user}!!")
        }
        def transform = new HenCoderTransform()
        def baseExtension = project.extensions.getByType(BaseExtension)
        baseExtension.registerTransform(transform)
    }
}