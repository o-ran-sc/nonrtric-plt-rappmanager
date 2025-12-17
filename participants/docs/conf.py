#  ============LICENSE_START======================================================================
#  Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
#  ===============================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END========================================================================

from docs_conf.conf import *

#branch configuration

branch = 'latest'

linkcheck_ignore = [
    'http://localhost.*',
    'http://127.0.0.1.*',
    'https://gerrit.o-ran-sc.org.*',
]

# Add some useful links available in every page
# Can be used later in any RST file as "<nonrtricwiki_>" etc.. (Note the underscores!)
rst_epilog = """
.. _nonrtricwiki: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/overview
.. _nonrtricwikidevguide: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903234/Release+M
.. _nonrtricwikik8s: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903652/Release+M+-+Run+in+Kubernetes
.. _nonrtricwikidocker: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903494/Release+M+-+Run+in+Docker
"""
## 

extensions = ['sphinx.ext.intersphinx',]


#intershpinx mapping with other projects
intersphinx_mapping = {}
intersphinx_mapping['nonrtric'] = ('https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric/en/%s' % branch, None)
intersphinx_disabled_reftypes = ["*"]

