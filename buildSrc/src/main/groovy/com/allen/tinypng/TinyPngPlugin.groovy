/**
 * Created by Wayne Yang
 * Copyright (c) 2015-present, mogujie.
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */

package com.allen.tinypng

import org.gradle.api.Plugin
import org.gradle.api.Project

class TinyPngPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "apply plugin :tinyPng"
        project.extensions.create("tinyInfo", TinyPngExtension)
        project.task("tinyPng", type: TinyPngTask)
        def taskPreBuild = project.tasks.getByName("preBuild")
        boolean enable = project.extensions.tinyInfo.enable
        if (enable) {
            taskPreBuild.dependsOn project.tasks.getByName('tinyPng')
        }
    }
}