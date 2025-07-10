const webpack = require('webpack');
const webpackMerge = require('webpack-merge').merge;
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin');
const path = require('path');
const sass = require('sass');

const commonConfig = require('./webpack.common');
const environment = require('./environment');

module.exports = async (config, options = {}) => {
  process.env.NODE_ENV = 'development';

  return webpackMerge(await commonConfig({ env: 'development' }), {
    entry: ['./src/main/webapp/app/index'],
    devtool: 'cheap-module-source-map',
    module: {
      rules: [
        {
          test: /\.(sa|sc|c)ss$/,
          use: [
            'style-loader',
            'css-loader',
            'postcss-loader',
            {
              loader: 'sass-loader',
              options: { implementation: sass },
            },
          ],
        },
      ],
    },
    output: {
      path: path.resolve(__dirname, '../target/classes/static/'),
      publicPath: '/',
    },
    devServer: {
      hot: true,
      static: {
        directory: './target/classes/static/',
      },
      port: 9060,
      proxy: [
        {
          context: ['/api', '/services', '/management', '/v3/api-docs', '/h2-console', '/auth', '/health'],
          target: `http${environment.tls ? 's' : ''}://localhost:8080`,
          secure: false,
          changeOrigin: environment.tls,
        },
      ],
      https: environment.tls,
      historyApiFallback: true,
    },
    stats: process.env.JHI_DISABLE_WEBPACK_LOGS ? 'none' : options.stats,
    plugins: [
      new SimpleProgressWebpackPlugin({
        format: 'compact',
      }),
      new BrowserSyncPlugin(
        {
          host: 'localhost',
          port: 9000,
          proxy: {
            target: `http://localhost:${options.port || 9060}`,
            proxyOptions: {
              changeOrigin: true,
            },
          },
          socket: {
            clients: {
              heartbeatTimeout: 60000,
            },
          },
          ghostMode: false,
        },
        {
          reload: false,
        },
      ),
    ].filter(Boolean),
  });
};
