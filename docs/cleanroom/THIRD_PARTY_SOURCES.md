# Retained native and engine notices

Rynx code is GPL-3.0-or-later. Third-party code retains its original licenses. Android packages the exact files from third_party_licenses as assets.

| Retained asset | Source / revision |
|---|---|
| Excor-LICENSE.txt, Excor-NOTICE.txt | Approved dependencies/eXcor LICENSE and NOTICE at its Git-pinned revision |
| Excor-EJS-LICENSE.txt | Approved dependencies/eXcor/ejs/LICENSE; EJS Unlicense, with meriyah/astring notices retained in Excor NOTICE |
| InnerTubeX-GPL-3.0.txt | [MetrolistGroup/innertubex v0.11.2](https://github.com/MetrolistGroup/innertubex/tree/v0.11.2), commit `c863ddf15be01a1abfa4630329bdc0a1e95f1531`; JitPack KMP dependency |
| QuackJS-GPL-3.0.txt, QuackJS-THIRD-PARTY-NOTICES.md | [MetrolistGroup/quackjs 0.1.2](https://github.com/MetrolistGroup/quackjs/tree/0.1.2), commit `5af5261d31e50b578e4c81434f6fce8dfd81db93`; transitive InnerTubeX dependency |
| QuickJS-kt-LICENSE.txt | https://raw.githubusercontent.com/dokar3/quickjs-kt/v1.0.15/LICENSE.txt ; Apache-2.0, Copyright 2015 Anton Lopyrev; README also credits Copyright 2024 dokar3 |
| QuickJS-LICENSE.txt | https://raw.githubusercontent.com/bellard/quickjs/04be246001599f5995fa2f2d8c91a0f198d3f34c/LICENSE ; MIT, Copyright 2017-2021 Fabrice Bellard and Charlie Gordon |
| c-vector-LICENSE.txt | https://raw.githubusercontent.com/eteran/c-vector/774773d4cb1e66dd736e90e7f482d4f22af464c6/LICENSE ; MIT, Copyright 2015 Evan Teran |

QuickJS native pins were verified from quickjs-kt v1.0.15 submodule metadata. Current AAR/POM metadata declares Apache-2.0 but does not itself package the native MIT texts, so Rynx retains them explicitly. Excor notice snapshots are synchronized by tools/refresh_excor.py and checked by tools/audit_workspace.py. The audit is procedural source/notice checking, not OS confinement or a full release compliance claim.
