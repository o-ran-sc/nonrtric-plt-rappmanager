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

selfbranch = 'latest'
participantsbranch = 'latest'  #sometimes this may be different from self.branch, e.g. on first build, it may not exist yet, so linkcheck will fail
nonrtricbranch = 'latest' #sometimes this may be different from self.branch, e.g. on first build, it may not exist yet, so linkcheck will fail
baseurl = 'https://docs.o-ran-sc.org/projects/'
selfurl = '%s/o-ran-sc-nonrtric-plt-rappmanager/en/%s' %(baseurl, selfbranch)
participantsurl = '%s/o-ran-sc-nonrtric-plt-rappmanager/en/%s/participants' %(baseurl, participantsbranch)
nonrtricurl = '%s/o-ran-sc-nonrtric/en/%s' %(baseurl, nonrtricbranch)

linkcheck_ignore = [
    'http://localhost.*',
    'http://127.0.0.1.*',
    'https://gerrit.o-ran-sc.org.*',
    './rappmanager-api.html', #Generated file that doesn't exist at link check.
]

extensions = ['sphinxcontrib.redoc', 'sphinx.ext.intersphinx',]

redoc = [
            {
                'name': 'rApp Manager API',
                'page': 'rappmanager-api',
                'spec': '../openapi/rappmanager/rappmanager-spec.yaml',
                'embed': True,
            }
        ]

redoc_uri = 'https://cdn.jsdelivr.net/npm/redoc@2.5.0/bundles/redoc.standalone.js'

#intershpinx mapping with other projects
intersphinx_mapping = {}

intersphinx_mapping['nonrtric'] = ('%s' %(nonrtricurl), None)
intersphinx_mapping['participants'] = ('%s' %(participantsurl), None)
intersphinx_disabled_reftypes = ["*"]

# Add some useful links available in every page
# Can be used later in any RST file as "<nonrtricwiki_>" etc.. (Note the underscores!)
rst_epilog = """
.. _nonrtricwiki: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/overview
.. _nonrtricwikidevguide: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903234/Release+M
.. _nonrtricwikik8s: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903652/Release+M+-+Run+in+Kubernetes
.. _nonrtricwikidocker: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/679903494/Release+M+-+Run+in+Docker
"""
## 
