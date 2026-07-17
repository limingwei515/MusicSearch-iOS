#!/usr/bin/env python3
"""生成符合 Xcode 标准格式的 project.pbxproj"""
import os
import uuid
import sys

ROOT = "/Users/youchangjiang/Documents/code/Android-MusicSearch-master/ios"
SRC_DIR = os.path.join(ROOT, "MusicSearch")

def gen_id():
    """生成 24 字符十六进制 ID"""
    return uuid.uuid4().hex[:24].upper()

# 收集所有 swift 文件
swift_files = []
for dirpath, dirnames, filenames in os.walk(SRC_DIR):
    dirnames[:] = [d for d in dirnames if d not in (".git", "Build", "DerivedData", "Assets.xcassets")]
    for f in sorted(filenames):
        if f.endswith(".swift"):
            rel = os.path.relpath(os.path.join(dirpath, f), SRC_DIR)
            swift_files.append(rel)

swift_files.sort()
print(f"Swift files: {len(swift_files)}", file=sys.stderr)

# 预分配 UUID
project_id = gen_id()
target_id = gen_id()
main_group_id = gen_id()
products_group_id = gen_id()
product_ref_id = gen_id()

sources_phase_id = gen_id()
resources_phase_id = gen_id()
frameworks_phase_id = gen_id()

proj_config_list_id = gen_id()
target_config_list_id = gen_id()
proj_debug_config_id = gen_id()
proj_release_config_id = gen_id()
target_debug_config_id = gen_id()
target_release_config_id = gen_id()

# 组 ID
groups = {}  # name -> id
group_names = ["App", "Models", "Crypto", "Utils", "Database", "Networking", "Player", "ViewModels", "Views", "Components", "Resources"]
for name in group_names:
    groups[name] = gen_id()

# 文件引用 ID
file_refs = {}  # path -> id
build_files = {}  # path -> id

for p in swift_files:
    file_refs[p] = gen_id()
    build_files[p] = gen_id()

# 资源文件
assets_id = gen_id()
assets_build_id = gen_id()
json_id = gen_id()
json_build_id = gen_id()
plist_id = gen_id()

# 按路径分组成员
def group_for(path):
    parts = path.split("/")
    if len(parts) >= 2:
        if parts[0] == "Views" and parts[1] == "Components":
            return "Components"
        return parts[0]
    return None

group_members = {name: [] for name in group_names}
for p in swift_files:
    g = group_for(p)
    if g:
        group_members[g].append(p)

# Views 组同时有子组和文件
views_files = [p for p in swift_files if p.startswith("Views/") and not p.startswith("Views/Components/")]
component_files = [p for p in swift_files if p.startswith("Views/Components/")]

# 开始生成
lines = []
def L(s=""):
    lines.append(s)

L("// !$*UTF8*$!")
L("{")
L("\tarchiveVersion = 1;")
L("\tclasses = {")
L("\t};")
L("\tobjectVersion = 56;")
L("\tobjects = {")
L("")

# ===== PBXBuildFile =====
L("/* Begin PBXBuildFile section */")
for p in swift_files:
    name = os.path.basename(p)
    L(f"\t\t{build_files[p]} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_refs[p]} /* {name} */; }};")
L(f"\t\t{assets_build_id} /* Assets.xcassets in Resources */ = {{isa = PBXBuildFile; fileRef = {assets_id} /* Assets.xcassets */; }};")
L(f"\t\t{json_build_id} /* updateLog.json in Resources */ = {{isa = PBXBuildFile; fileRef = {json_id} /* updateLog.json */; }};")
L("/* End PBXBuildFile section */")
L("")

# ===== PBXFileReference =====
L("/* Begin PBXFileReference section */")
for p in swift_files:
    name = os.path.basename(p)
    L(f"\t\t{file_refs[p]} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = \"{p}\"; sourceTree = \"<group>\"; }};")
L(f"\t\t{assets_id} /* Assets.xcassets */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = \"<group>\"; }};")
L(f"\t\t{json_id} /* updateLog.json */ = {{isa = PBXFileReference; lastKnownFileType = text.json; path = updateLog.json; sourceTree = \"<group>\"; }};")
L(f"\t\t{plist_id} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = \"<group>\"; }};")
L(f"\t\t{product_ref_id} /* MusicSearch.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = MusicSearch.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
L("/* End PBXFileReference section */")
L("")

# ===== PBXFrameworksBuildPhase =====
L("/* Begin PBXFrameworksBuildPhase section */")
L(f"\t\t{frameworks_phase_id} /* Frameworks */ = {{")
L("\t\t\tisa = PBXFrameworksBuildPhase;")
L("\t\t\tbuildActionMask = 2147483647;")
L("\t\t\tfiles = (")
L("\t\t\t);")
L("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
L("\t\t};")
L("/* End PBXFrameworksBuildPhase section */")
L("")

# ===== PBXGroup =====
L("/* Begin PBXGroup section */")

# 主组
L(f"\t\t{main_group_id} = {{")
L("\t\t\tisa = PBXGroup;")
L("\t\t\tchildren = (")
for name in ["App", "Models", "Crypto", "Utils", "Database", "Networking", "Player", "ViewModels", "Views", "Resources"]:
    L(f"\t\t\t\t{groups[name]} /* {name} */,")
L(f"\t\t\t\t{assets_id} /* Assets.xcassets */,")
L(f"\t\t\t\t{plist_id} /* Info.plist */,")
L("\t\t\t);")
L(f"\t\t\tpath = MusicSearch;")
L("\t\t\tsourceTree = \"<group>\";")
L("\t\t};")

# Products 组
L(f"\t\t{products_group_id} /* Products */ = {{")
L("\t\t\tisa = PBXGroup;")
L("\t\t\tchildren = (")
L(f"\t\t\t\t{product_ref_id} /* MusicSearch.app */,")
L("\t\t\t);")
L("\t\t\tname = Products;")
L("\t\t\tsourceTree = \"<group>\";")
L("\t\t};")

# 各子组
for name in group_names:
    if name == "Views":
        L(f"\t\t{groups[name]} /* {name} */ = {{")
        L("\t\t\tisa = PBXGroup;")
        L("\t\t\tchildren = (")
        L(f"\t\t\t\t{groups['Components']} /* Components */,")
        for p in views_files:
            fname = os.path.basename(p)
            L(f"\t\t\t\t{file_refs[p]} /* {fname} */,")
        L("\t\t\t);")
        L(f"\t\t\tname = {name};")
        L("\t\t\tsourceTree = \"<group>\";")
        L("\t\t};")
    elif name == "Components":
        L(f"\t\t{groups[name]} /* {name} */ = {{")
        L("\t\t\tisa = PBXGroup;")
        L("\t\t\tchildren = (")
        for p in component_files:
            fname = os.path.basename(p)
            L(f"\t\t\t\t{file_refs[p]} /* {fname} */,")
        L("\t\t\t);")
        L(f"\t\t\tname = {name};")
        L("\t\t\tsourceTree = \"<group>\";")
        L("\t\t};")
    elif name == "Resources":
        L(f"\t\t{groups[name]} /* {name} */ = {{")
        L("\t\t\tisa = PBXGroup;")
        L("\t\t\tchildren = (")
        L(f"\t\t\t\t{json_id} /* updateLog.json */,")
        L("\t\t\t);")
        L(f"\t\t\tpath = {name};")
        L("\t\t\tsourceTree = \"<group>\";")
        L("\t\t};")
    else:
        L(f"\t\t{groups[name]} /* {name} */ = {{")
        L("\t\t\tisa = PBXGroup;")
        L("\t\t\tchildren = (")
        for p in group_members[name]:
            fname = os.path.basename(p)
            L(f"\t\t\t\t{file_refs[p]} /* {fname} */,")
        L("\t\t\t);")
        L(f"\t\t\tname = {name};")
        L("\t\t\tsourceTree = \"<group>\";")
        L("\t\t};")

L("/* End PBXGroup section */")
L("")

# ===== PBXNativeTarget =====
L("/* Begin PBXNativeTarget section */")
L(f"\t\t{target_id} /* MusicSearch */ = {{")
L("\t\t\tisa = PBXNativeTarget;")
L(f"\t\t\tbuildConfigurationList = {target_config_list_id} /* Build configuration list for PBXNativeTarget \"MusicSearch\" */;")
L("\t\t\tbuildPhases = (")
L(f"\t\t\t\t{sources_phase_id} /* Sources */,")
L(f"\t\t\t\t{frameworks_phase_id} /* Frameworks */,")
L(f"\t\t\t\t{resources_phase_id} /* Resources */,")
L("\t\t\t);")
L("\t\t\tbuildRules = (")
L("\t\t\t);")
L("\t\t\tdependencies = (")
L("\t\t\t);")
L("\t\t\tname = MusicSearch;")
L("\t\t\tproductName = MusicSearch;")
L(f"\t\t\tproductReference = {product_ref_id} /* MusicSearch.app */;")
L("\t\t\tproductType = \"com.apple.product-type.application\";")
L("\t\t};")
L("/* End PBXNativeTarget section */")
L("")

# ===== PBXProject =====
L("/* Begin PBXProject section */")
L(f"\t\t{project_id} /* Project object */ = {{")
L("\t\t\tisa = PBXProject;")
L("\t\t\tattributes = {")
L("\t\t\t\tBuildIndependentTargetsInParallel = 1;")
L(f"\t\t\t\tLastUpgradeCheck = 1600;")
L("\t\t\t\tTargetAttributes = {")
L(f"\t\t\t\t\t{target_id} = {{")
L("\t\t\t\t\t\tCreatedOnToolsVersion = 16.0;")
L("\t\t\t\t\t};")
L("\t\t\t\t};")
L("\t\t\t};")
L(f"\t\t\tbuildConfigurationList = {proj_config_list_id} /* Build configuration list for PBXProject \"MusicSearch\" */;")
L("\t\t\tcompatibilityVersion = \"Xcode 14.0\";")
L("\t\t\tdevelopmentRegion = en;")
L("\t\t\thasScannedForEncodings = 0;")
L("\t\t\tknownRegions = (")
L("\t\t\t\ten,")
L("\t\t\t\tBase,")
L("\t\t\t);")
L(f"\t\t\tmainGroup = {main_group_id};")
L(f"\t\t\tproductRefGroup = {products_group_id} /* Products */;")
L("\t\t\tprojectDirPath = \"\";")
L("\t\t\tprojectRoot = \"\";")
L("\t\t\ttargets = (")
L(f"\t\t\t\t{target_id} /* MusicSearch */,")
L("\t\t\t);")
L("\t\t};")
L("/* End PBXProject section */")
L("")

# ===== PBXResourcesBuildPhase =====
L("/* Begin PBXResourcesBuildPhase section */")
L(f"\t\t{resources_phase_id} /* Resources */ = {{")
L("\t\t\tisa = PBXResourcesBuildPhase;")
L("\t\t\tbuildActionMask = 2147483647;")
L("\t\t\tfiles = (")
L(f"\t\t\t\t{assets_build_id} /* Assets.xcassets in Resources */,")
L(f"\t\t\t\t{json_build_id} /* updateLog.json in Resources */,")
L("\t\t\t);")
L("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
L("\t\t};")
L("/* End PBXResourcesBuildPhase section */")
L("")

# ===== PBXSourcesBuildPhase =====
L("/* Begin PBXSourcesBuildPhase section */")
L(f"\t\t{sources_phase_id} /* Sources */ = {{")
L("\t\t\tisa = PBXSourcesBuildPhase;")
L("\t\t\tbuildActionMask = 2147483647;")
L("\t\t\tfiles = (")
for p in swift_files:
    fname = os.path.basename(p)
    L(f"\t\t\t\t{build_files[p]} /* {fname} in Sources */,")
L("\t\t\t);")
L("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
L("\t\t};")
L("/* End PBXSourcesBuildPhase section */")
L("")

# ===== XCBuildConfiguration - Project Debug =====
L("/* Begin XCBuildConfiguration section */")
L(f"\t\t{proj_debug_config_id} /* Debug */ = {{")
L("\t\t\tisa = XCBuildConfiguration;")
L("\t\t\tbuildSettings = {")
L("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
L("\t\t\t\tCLANG_ANALYZER_NONNULL = YES;")
L("\t\t\t\tCLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;")
L("\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = \"gnu++20\";")
L("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
L("\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;")
L("\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;")
L("\t\t\t\tCLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;")
L("\t\t\t\tCLANG_WARN_BOOL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_COMMA = YES;")
L("\t\t\t\tCLANG_WARN_CONSTANT_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;")
L("\t\t\t\tCLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;")
L("\t\t\t\tCLANG_WARN_DOCUMENTATION_COMMENTS = YES;")
L("\t\t\t\tCLANG_WARN_EMPTY_BODY = YES;")
L("\t\t\t\tCLANG_WARN_ENUM_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_INFINITE_RECURSION = YES;")
L("\t\t\t\tCLANG_WARN_INT_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_LITERAL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;")
L("\t\t\t\tCLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;")
L("\t\t\t\tCLANG_WARN_RANGE_LOOP_ANALYSIS = YES;")
L("\t\t\t\tCLANG_WARN_STRICT_PROTOTYPES = YES;")
L("\t\t\t\tCLANG_WARN_SUSPICIOUS_MOVE = YES;")
L("\t\t\t\tCLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;")
L("\t\t\t\tCLANG_WARN_UNREACHABLE_CODE = YES;")
L("\t\t\t\tCLANG_WARN__DUPLICATE_METHOD_MATCH = YES;")
L("\t\t\t\tCOPY_PHASE_STRIP = NO;")
L("\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;")
L("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
L("\t\t\t\tENABLE_TESTABILITY = YES;")
L("\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu17;")
L("\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;")
L("\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;")
L("\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;")
L("\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (")
L("\t\t\t\t\t\"DEBUG=1\",")
L("\t\t\t\t\t\"$(inherited)\",")
L("\t\t\t\t);")
L("\t\t\t\tGCC_WARN_64_TO_32_BIT_CONVERSION = YES;")
L("\t\t\t\tGCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;")
L("\t\t\t\tGCC_WARN_UNDECLARED_SELECTOR = YES;")
L("\t\t\t\tGCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;")
L("\t\t\t\tGCC_WARN_UNUSED_FUNCTION = YES;")
L("\t\t\t\tGCC_WARN_UNUSED_VARIABLE = YES;")
L("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
L("\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;")
L("\t\t\t\tMTL_FAST_MATH = YES;")
L("\t\t\t\tONLY_ACTIVE_ARCH = YES;")
L("\t\t\t\tSDKROOT = iphoneos;")
L("\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = \"$(inherited) DEBUG\";")
L("\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-Onone\";")
L("\t\t\t};")
L("\t\t\tname = Debug;")
L("\t\t};")

# Project Release
L(f"\t\t{proj_release_config_id} /* Release */ = {{")
L("\t\t\tisa = XCBuildConfiguration;")
L("\t\t\tbuildSettings = {")
L("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
L("\t\t\t\tCLANG_ANALYZER_NONNULL = YES;")
L("\t\t\t\tCLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;")
L("\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = \"gnu++20\";")
L("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
L("\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;")
L("\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;")
L("\t\t\t\tCLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;")
L("\t\t\t\tCLANG_WARN_BOOL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_COMMA = YES;")
L("\t\t\t\tCLANG_WARN_CONSTANT_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;")
L("\t\t\t\tCLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;")
L("\t\t\t\tCLANG_WARN_DOCUMENTATION_COMMENTS = YES;")
L("\t\t\t\tCLANG_WARN_EMPTY_BODY = YES;")
L("\t\t\t\tCLANG_WARN_ENUM_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_INFINITE_RECURSION = YES;")
L("\t\t\t\tCLANG_WARN_INT_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_LITERAL_CONVERSION = YES;")
L("\t\t\t\tCLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;")
L("\t\t\t\tCLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;")
L("\t\t\t\tCLANG_WARN_RANGE_LOOP_ANALYSIS = YES;")
L("\t\t\t\tCLANG_WARN_STRICT_PROTOTYPES = YES;")
L("\t\t\t\tCLANG_WARN_SUSPICIOUS_MOVE = YES;")
L("\t\t\t\tCLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;")
L("\t\t\t\tCLANG_WARN_UNREACHABLE_CODE = YES;")
L("\t\t\t\tCLANG_WARN__DUPLICATE_METHOD_MATCH = YES;")
L("\t\t\t\tCOPY_PHASE_STRIP = NO;")
L("\t\t\t\tDEBUG_INFORMATION_FORMAT = \"dwarf-with-dsym\";")
L("\t\t\t\tENABLE_NS_ASSERTIONS = NO;")
L("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
L("\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu17;")
L("\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;")
L("\t\t\t\tGCC_WARN_64_TO_32_BIT_CONVERSION = YES;")
L("\t\t\t\tGCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;")
L("\t\t\t\tGCC_WARN_UNDECLARED_SELECTOR = YES;")
L("\t\t\t\tGCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;")
L("\t\t\t\tGCC_WARN_UNUSED_FUNCTION = YES;")
L("\t\t\t\tGCC_WARN_UNUSED_VARIABLE = YES;")
L("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
L("\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;")
L("\t\t\t\tMTL_FAST_MATH = YES;")
L("\t\t\t\tSDKROOT = iphoneos;")
L("\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;")
L("\t\t\t\tVALIDATE_PRODUCT = YES;")
L("\t\t\t};")
L("\t\t\tname = Release;")
L("\t\t};")

# Target Debug
L(f"\t\t{target_debug_config_id} /* Debug */ = {{")
L("\t\t\tisa = XCBuildConfiguration;")
L("\t\t\tbuildSettings = {")
L("\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;")
L("\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;")
L("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
L("\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
L("\t\t\t\tDEVELOPMENT_ASSET_PATHS = \"\\\"\\\"\";")
L("\t\t\t\tENABLE_PREVIEWS = YES;")
L("\t\t\t\tGENERATE_INFOPLIST_FILE = NO;")
L("\t\t\t\tINFOPLIST_FILE = MusicSearch/Info.plist;")
L("\t\t\t\tINFOPLIST_KEY_CFBundleDisplayName = \"音乐搜索\";")
L("\t\t\t\tINFOPLIST_KEY_UILaunchScreen_Generation = YES;")
L("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = \"UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\";")
L("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = \"UIInterfaceOrientationPortrait\";")
L("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
L("\t\t\t\t\t\"$(inherited)\",")
L("\t\t\t\t\t\"@executable_path/Frameworks\",")
L("\t\t\t\t);")
L("\t\t\t\tMARKETING_VERSION = 1.0;")
L("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.linfeng.musicsearch;")
L("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
L("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
L("\t\t\t\tSWIFT_VERSION = 5.0;")
L("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
L("\t\t\t};")
L("\t\t\tname = Debug;")
L("\t\t};")

# Target Release
L(f"\t\t{target_release_config_id} /* Release */ = {{")
L("\t\t\tisa = XCBuildConfiguration;")
L("\t\t\tbuildSettings = {")
L("\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;")
L("\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;")
L("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
L("\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
L("\t\t\t\tDEVELOPMENT_ASSET_PATHS = \"\\\"\\\"\";")
L("\t\t\t\tENABLE_PREVIEWS = YES;")
L("\t\t\t\tGENERATE_INFOPLIST_FILE = NO;")
L("\t\t\t\tINFOPLIST_FILE = MusicSearch/Info.plist;")
L("\t\t\t\tINFOPLIST_KEY_CFBundleDisplayName = \"音乐搜索\";")
L("\t\t\t\tINFOPLIST_KEY_UILaunchScreen_Generation = YES;")
L("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations_iPad = \"UIInterfaceOrientationPortrait UIInterfaceOrientationPortraitUpsideDown UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight\";")
L("\t\t\t\tINFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone = \"UIInterfaceOrientationPortrait\";")
L("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
L("\t\t\t\t\"$(inherited)\",")
L("\t\t\t\t\t\"@executable_path/Frameworks\",")
L("\t\t\t\t);")
L("\t\t\t\tMARKETING_VERSION = 1.0;")
L("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.linfeng.musicsearch;")
L("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
L("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
L("\t\t\t\tSWIFT_VERSION = 5.0;")
L("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
L("\t\t\t};")
L("\t\t\tname = Release;")
L("\t\t};")
L("/* End XCBuildConfiguration section */")
L("")

# ===== XCConfigurationList =====
L("/* Begin XCConfigurationList section */")
L(f"\t\t{proj_config_list_id} /* Build configuration list for PBXProject \"MusicSearch\" */ = {{")
L("\t\t\tisa = XCConfigurationList;")
L("\t\t\tbuildConfigurations = (")
L(f"\t\t\t\t{proj_debug_config_id} /* Debug */,")
L(f"\t\t\t\t{proj_release_config_id} /* Release */,")
L("\t\t\t);")
L("\t\t\tdefaultConfigurationIsVisible = 0;")
L("\t\t\tdefaultConfigurationName = Release;")
L("\t\t};")
L(f"\t\t{target_config_list_id} /* Build configuration list for PBXNativeTarget \"MusicSearch\" */ = {{")
L("\t\t\tisa = XCConfigurationList;")
L("\t\t\tbuildConfigurations = (")
L(f"\t\t\t\t{target_debug_config_id} /* Debug */,")
L(f"\t\t\t\t{target_release_config_id} /* Release */,")
L("\t\t\t);")
L("\t\t\tdefaultConfigurationIsVisible = 0;")
L("\t\t\tdefaultConfigurationName = Release;")
L("\t\t};")
L("/* End XCConfigurationList section */")
L("")

L("\t};")
L(f"\trootObject = {project_id} /* Project object */;")
L("}")

# 写入
pbxproj_path = os.path.join(ROOT, "MusicSearch.xcodeproj", "project.pbxproj")
with open(pbxproj_path, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")

print(f"Generated: {pbxproj_path}")
print(f"Total lines: {len(lines)}")
