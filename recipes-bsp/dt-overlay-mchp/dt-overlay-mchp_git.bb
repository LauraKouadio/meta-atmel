DESCRIPTION = "Compile AT91 board device tree overlays and pack them in a FIT image"
LICENSE = "(GPLv2+ & MIT)"
LIC_FILES_CHKSUM = "file://COPYING;md5=775626b7bc958bdcc525161f725ece0f \
                    file://LICENSES/GPL-2.0;md5=e6a75371ba4d16749254a51215d13f97 \
                    file://LICENSES/MIT;md5=e8f57dd048e186199433be2c41bd3d6d"

inherit deploy

COMPATIBLE_MACHINE = '(sam9x60-curiosity|sam9x60-curiosity-sd)'

SRC_URI = "git://github.com/linux4microchip/dt-overlay-mchp.git;protocol=https"

PV = "1.0+git${SRCPV}"
SRCREV = "712ddeb699b9c51782d32b6bc9abb5164c79e294"

DEPENDS = "virtual/kernel u-boot-mkimage-native dtc-native"

S = "${WORKDIR}/git"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Ensure that the machine is properly set
AT91BOOTSTRAP_MACHINE ??= "${MACHINE}"

do_compile[depends] += "virtual/kernel:do_deploy virtual/kernel:do_shared_workdir"
do_compile[nostamp] = "1"

do_compile () {
    # Check to properly identify the board
    if [ -z "${AT91BOOTSTRAP_MACHINE}" ]; then
        echo "No AT91BOOTSTRAP_MACHINE set for ${MACHINE}"
        exit 1
    fi

    if [ -d "${AT91BOOTSTRAP_MACHINE}" ]; then
        echo "Compiling DTBOs"
        oe_runmake DTC=dtc KERNEL_DIR=${STAGING_KERNEL_DIR} KERNEL_BUILD_DIR=${KERNEL_PATH} ${AT91BOOTSTRAP_MACHINE}_dtbos
    else
        echo "No DTBOs to compile"
    fi

    # Over-ride itb target in Makefile
    if [ -e "${AT91BOOTSTRAP_MACHINE}.its" ]; then
        echo "Creating the FIT image"
        DTC_OPTIONS="-Wno-unit_address_vs_reg -Wno-graph_child_address -Wno-pwms_property"
        mkimage -D "-i${DEPLOY_DIR_IMAGE} -p 1000 ${DTC_OPTIONS}" -f ${AT91BOOTSTRAP_MACHINE}.its ${AT91BOOTSTRAP_MACHINE}.itb
    else
        echo "No its file to create FIT images"
    fi
}

FILES_${PN} = "/boot/*.*"

addtask install after do_compile

do_install () {
    # Copy files to /boot
    if [ -e ${AT91BOOTSTRAP_MACHINE} ]; then
        install -d ${D}/boot
        install ${AT91BOOTSTRAP_MACHINE}/${AT91BOOTSTRAP_MACHINE}*.dtbo ${D}/boot
    fi;

    if [ -e ${AT91BOOTSTRAP_MACHINE}.itb ]; then
        install -d ${D}/boot
        install ${AT91BOOTSTRAP_MACHINE}.itb ${D}/boot/
        install ${AT91BOOTSTRAP_MACHINE}.its ${D}/boot/
    fi;
}

addtask deploy after do_install

do_deploy () {
    #echo "Copying ${fit_image_basename}.itb and source file to ${DEPLOYDIR}..."
    if [ -e ${AT91BOOTSTRAP_MACHINE}.itb ]; then
        install ${AT91BOOTSTRAP_MACHINE}.itb ${DEPLOYDIR}/
        install ${AT91BOOTSTRAP_MACHINE}.its ${DEPLOYDIR}/
    fi;
}
