// MapView.js

import React from 'react';
import PropTypes from 'prop-types';
import {
  View,
  Button,
  Text,
  NativeModules,
  NativeEventEmitter,
  DeviceEventEmitter,
  requireNativeComponent,
  findNodeHandle
} from 'react-native';





const MAP_TYPES = {
  NORMAL: 'normal',
  SATELLITE: 'satellite',
};

const MapViewComponent = NativeModules.MapView;
const UIManager = NativeModules.UIManager;

// const eventEmitter = new NativeEventEmitter(MapViewComponent);

// eventEmitter.addListener('onSessionConnect', onSessionConnect);

let iface = {
  name: 'MapView',
  propTypes: {
    //...View.propTypes, // include the default view properties

    /**
     * style
     */
    style: PropTypes.Object,

    /**
     * Center of the map : latitude,longitude
     */
    center: PropTypes.Object,

    /**
     * The map type to be displayed.
     *
     * - standard: standard road map (default)
     * - satellite: satellite view
     * - hybrid: satellite view with roads and points of interest overlayed
     * - terrain: topographic view
     * - none: no base map
     */
    mapType: PropTypes.oneOf(Object.values(MAP_TYPES)),

    /**
     * initialZoom: initial zoom level for map
     */
     initialZoom : PropTypes.number,

     /**
     * Marker put on the map for the user location : latitude,longitude
     */
    userLocation: PropTypes.Object,

    markersList: PropTypes.array,
  },
};

// requireNativeComponent commonly takes two parameters, the first is
// the name of the native view and the second is an object that describes the
// component interface. The component interface should declare a friendly name
// for use in debug messages and must declare the propTypes reflected by the
// Native View. The propTypes are used for checking the validity of a user's
// use of the native view.
// Note that if you need your JavaScript component to do
// more than just specify a name and propTypes, like do custom event handling,
// you can wrap the native component in a normal react component. In that case,
// you want to pass in the wrapper component instead of iface to
// requireNativeComponent.
const HereMapView = requireNativeComponent('HereMapView', iface);

export default class MapView extends React.Component {

  eventEmitter = new NativeEventEmitter(MapViewComponent);

  constructor(props) {
    super(props);

    this.state = {
      isReady: false,
      zoomLevel: this.props.zoom || 15,
      center: this.props.center
    };
    this.eventEmitter.addListener('onMoveMap', this.onMoveMap);
    this.eventEmitter.addListener('onMarkerClicked', this.onMarkerClicked);
    this._onMapReady = this._onMapReady.bind(this);
  }

  // componentWillUpdate() is invoked immediately before rendering when
  // new props or state are being received. Use this as an opportunity
  // to perform preparation before an update occurs. This method is not called
  // for the initial render.
  componentWillUpdate(nextProps) {
  }


  // componentDidMount() is invoked immediately after a component is mounted.
  // Initialization that requires DOM nodes should go here. If you need to load
  // data from a remote endpoint, this is a good place to instantiate the
  // network request. Setting state in this method will trigger a re-rendering.
  componentDidMount() {
    const { isReady } = this.state;
    if (isReady) {
    }
  }

  render() {
    return (
      
      <HereMapView
        style={ this.props.style }
        center={this.props.center}
        mapType={ this.props.mapType }
        initialZoom={ this.state.zoomLevel } 
        userLocation={this.props.userLocation}
        markersList={this.props.markersList} >

        <View style={{ position:'absolute', top: 10, right: 10,
                       width: 50, height: 120,
                       justifyContent: 'space-between', zIndex:10 }}>
        </View>
      </HereMapView>
    );
  }

  onMoveMap = (event) => {
    this.props.onMove ? this.props.onMove(event) : null;
   };
   onMarkerClicked = (event) => {
    this.props.onMarkerClicked ? this.props.onMarkerClicked(event) : null;
   };

  _onMapReady() {
    this.setState({ isReady: true });
  }

  onZoomInPress = () => {
    if ( this.state.zoomLevel < 20 ) {
      this.setState({ zoomLevel : this.state.zoomLevel + 1});
      UIManager.dispatchViewManagerCommand(
          findNodeHandle(this),
          UIManager.HereMapView.Commands.zoomIn,
          [this.state.zoomLevel] );
    }
  }

  onZoomOutPress = () => {
    if (this.state.zoomLevel > 0) {
      this.setState({ zoomLevel : this.state.zoomLevel - 1});
      UIManager.dispatchViewManagerCommand(
          findNodeHandle(this),
          UIManager.HereMapView.Commands.zoomOut,
          [ this.state.zoomLevel ] );
    }
  }


  onSetCenterPress = (center) => {
    this.setState({ center : center });
    UIManager.dispatchViewManagerCommand(
        findNodeHandle(this),
        UIManager.HereMapView.Commands.setCenter,
        [ center ] );
  }

}
