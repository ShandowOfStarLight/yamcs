import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { filter, map, take, switchMap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { selectInstancesLoaded, selectInstancesById } from '../store/instance.selectors';

@Injectable()
export class InstanceExistsGuard implements CanActivate {

  constructor(private store: Store<any>, private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const instanceId = route.params['instance'];

    // When the instances are loaded, verify the store contains
    // a matching entity.
    return this.store.select(selectInstancesLoaded).pipe(
      filter(loaded => loaded),
      take(1),
      switchMap(() => this.storeContainsInstance(instanceId)),
    );
  }

  storeContainsInstance(id: string): Observable<boolean> {
    return this.store.select(selectInstancesById).pipe(
      map(entities => !!entities[id]),
      take(1),
      switchMap(inStore => {
        if (!inStore) {
          this.router.navigate(['/404'], {
            // Would like the attempted URL to stay in the browser address bar
            // but unfortunately below property does not work. Follow this issue:
            // https://github.com/angular/angular/issues/17004
            //
            // skipLocationChange: true
          });
        }
        return of(inStore);
      }),
    );
  }
}
