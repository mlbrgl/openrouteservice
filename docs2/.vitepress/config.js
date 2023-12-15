import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    title: "openrouteservice backend documentation",
    description: "openrouteservice backend documentation",
    base: "/openrouteservice/",
    head: [['link', {rel: 'icon', href: '/openrouteservice/ors_fav.png'}]],
    cleanUrls: true,
    themeConfig: {
        // https://vitepress.dev/reference/default-theme-config
        siteTitle: false,
        logo: {
            light: '/openrouteservice.png',
            dark: '/openrouteservice_dark.png',
            alt: 'openrouteservice logo'
        },
        search: {
            provider: 'local'
        },
        outline: {
            level: [2, 4]
        },
        lastUpdated: {
            text: 'Updated at',
            formatOptions: {
                dateStyle: 'medium',
                timeStyle: 'short'
            }
        },
        editLink: {
            pattern: 'https://github.com/GIScience/openrouteservice/issues/new?labels=documentation+%3Abook%3A&template=docs.yml',
            text: 'Suggest an improvement'
        },
        nav: [
            {text: 'Homepage', link: 'https://openrouteservice.org'},
            {text: 'Forum', link: 'https://ask.openrouteservice.org'},
            {text: 'API Playground', link: 'https://openrouteservice.org/dev/#/api-docs'},
        ],
        sidebar: [
            {
                text: 'Home', link: '/',
                items: [
                    {text: 'Getting Started', link: '/getting-started'},
                    {
                        text: 'API Reference', collapsed: true, link: '/api-reference/',
                        items: [
                            {
                                text: 'Endpoints', collapsed: true, link: '/api-reference/endpoints/',
                                items: [
                                    {
                                        text: 'Directions', collapsed: true, link: '/api-reference/endpoints/directions/',
                                        items: [
                                            {text: 'Requests and Return Types', link: '/api-reference/endpoints/directions/requests-and-return-types'},
                                            {text: 'Routing Options', link: '/api-reference/endpoints/directions/routing-options'},
                                            {
                                                text: 'Extra info', collapsed: true, link: '/api-reference/endpoints/directions/extra-info/',
                                                items: [
                                                    {text: 'Steepness IDs', link: '/api-reference/endpoints/directions/extra-info/steepness'},
                                                    {text: 'Surface IDs', link: '/api-reference/endpoints/directions/extra-info/surface'},
                                                    {text: 'Category IDs', link: '/api-reference/endpoints/directions/extra-info/waycategory'},
                                                    {text: 'Type IDs', link: '/api-reference/endpoints/directions/extra-info/waytype'},
                                                    {text: 'Difficulty IDs', link: '/api-reference/endpoints/directions/extra-info/trail-difficulty'},
                                                    {text: 'Restriction IDs', link: '/api-reference/endpoints/directions/extra-info/road-access-restrictions'},
                                                    {text: 'Country IDs', link: '/technical-details/country-list'},
                                                ]
                                            },
                                            {text: 'Route Attributes', link: '/api-reference/endpoints/directions/route-attributes'},
                                            {text: 'Geometry Decoding', link: '/api-reference/endpoints/directions/geometry-decoding'},
                                            {text: 'Instruction Types', link: '/api-reference/endpoints/directions/instruction-types'},
                                        ]
                                    },
                                    {text: 'Isochrones', link: '/api-reference/endpoints/isochrones/'},
                                    {text: 'Matrix', link: '/api-reference/endpoints/matrix/'},
                                    {text: 'Snapping', link: '/api-reference/endpoints/snapping/'},
                                    {text: 'Export', link: '/api-reference/endpoints/export/'},
                                    {text: 'Health', link: '/api-reference/endpoints/health/'},
                                    {text: 'Status', link: '/api-reference/endpoints/status/'},
                                ]
                            },
                            {text: 'Error Codes', link: '/api-reference/error-codes'},
                        ]
                    },
                    {
                        text: 'Run ORS instance', collapsed: true, link: '/run-instance/',
                        items: [
                            {text: 'System Requirements', link: '/run-instance/system-requirements'},
                            {text: 'Data', link: '/run-instance/data'},
                            {
                                text: 'Installation', collapsed: true, link: '/run-instance/installation/',
                                items: [
                                    {text: 'Running JAR / WAR', link: '/run-instance/installation/running-jar-war'},
                                    {text: 'Running within a container', link: '/run-instance/installation/running-in-container'},
                                    {text: 'Building from Source', link: '/run-instance/installation/building-from-source'},
                                ]
                            },
                            {
                                text: 'Configuration', collapsed: true, link: '/run-instance/configuration/',
                                items: [
                                    {text: 'Endpoints and limits', link: '/run-instance/configuration/endpoints-and-limits'},
                                    {text: 'Profiles', link: '/run-instance/configuration/profiles'},
                                    {text: 'Extras', link: '/run-instance/configuration/extras'},
                                    {text: 'Logging', link: '/run-instance/configuration/logging'},
                                    {text: 'Spring', link: '/run-instance/configuration/spring'}
                                ]
                            },
                        ]
                    },
                    {
                        text: 'Contributing', collapsed: true, link: '/contributing/',
                        items: [
                            {text: 'Opening project in IntelliJ', link: '/contributing/opening-project-in-intellij'},
                            {text: 'Backend documentation', link: '/contributing/backend-documentation'},
                            {text: 'Contribution guidelines', link: '/contributing/contribution-guidelines'},
                            {text: 'Contributing translations', link: '/contributing/contributing-translations'},
                        ]
                    },
                    {
                        text: 'Technical details', collapsed: true, link: '/technical-details/',
                        items: [
                            {text: 'Country list', link: '/technical-details/country-list'},
                            {text: 'Travel speeds', link: '/technical-details/travel-speeds/',
                                items: [
                                    {text: 'Country Speeds', link: '/technical-details/travel-speeds/country-speeds.md'},
                                    {text: 'Tracktype Speeds', link: '/technical-details/travel-speeds/tracktype-speeds.md'},
                                    {text: 'Waytype Speeds', link: '/technical-details/travel-speeds/waytype-speeds.md'},
                                    {text: 'Surface Speeds', link: '/technical-details/travel-speeds/surface-speeds.md'},
                                ]
                            },
                            {text: 'Tag filtering', link: '/technical-details/tag-filtering'}
                        ]
                    },
                    {text: 'FAQ', link: '/frequently-asked-questions'}
                ]
            }
        ],
        socialLinks: [
            {icon: 'github', link: 'https://github.com/GIScience/openrouteservice'}
        ],
        footer: {
            message: '<a href="https://openrouteservice.org/">openrouteservice</a> is part of <a href="https://heigit.org/">HeiGIT gGmbH</a> and Universität Heidelberg <a href="https://www.geog.uni-heidelberg.de/gis/index_en.html">GIScience</a> research group. | <a href="https://heigit.org/imprint/">Imprint</a>'
        }
    }
})
